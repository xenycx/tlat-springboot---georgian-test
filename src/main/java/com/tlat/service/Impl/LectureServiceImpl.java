package com.tlat.service.Impl;

import com.tlat.Dto.LectureDto;
import com.tlat.Entity.Lecture;
import com.tlat.Entity.LearningResource;
import com.tlat.Entity.LectureSchedule;
import com.tlat.Entity.LectureStatus;
import com.tlat.Entity.Room;
import com.tlat.Entity.StudentGroup;
import com.tlat.Entity.User;
import com.tlat.Repository.LearningResourceRepository;
import com.tlat.Repository.LectureRepository;
import com.tlat.Repository.LectureScheduleRepository;
import com.tlat.Repository.RoomRepository;
import com.tlat.Repository.StudentGroupRepository;
import com.tlat.Repository.UserRepository;
import com.tlat.service.IpVerificationService;
import com.tlat.service.LearningResourceStorageService;
import com.tlat.service.LectureService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LectureServiceImpl implements LectureService {

    private static final Logger logger = LoggerFactory.getLogger(LectureServiceImpl.class);

    private final LectureRepository lectureRepository;
    private final LectureScheduleRepository lectureScheduleRepository;
    private final RoomRepository roomRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final IpVerificationService ipVerificationService;
    private final LearningResourceRepository learningResourceRepository;
    private final LearningResourceStorageService learningResourceStorageService;

    @Autowired
    public LectureServiceImpl(LectureRepository lectureRepository,
                              LectureScheduleRepository lectureScheduleRepository,
                              RoomRepository roomRepository,
                              StudentGroupRepository studentGroupRepository,
                              UserRepository userRepository,
                              IpVerificationService ipVerificationService,
                              LearningResourceRepository learningResourceRepository,
                              LearningResourceStorageService learningResourceStorageService) {
        this.lectureRepository = lectureRepository;
        this.lectureScheduleRepository = lectureScheduleRepository;
        this.roomRepository = roomRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
        this.ipVerificationService = ipVerificationService;
        this.learningResourceRepository = learningResourceRepository;
        this.learningResourceStorageService = learningResourceStorageService;
    }

    @Scheduled(fixedRate = 60000)
    public void updateLectureStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<LectureSchedule> schedules = lectureScheduleRepository.findAll();

        for (LectureSchedule schedule : schedules) {
            LocalDateTime scheduleEnd = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());
            if (schedule.getStatus() == LectureStatus.SCHEDULED && now.isAfter(scheduleEnd)) {
                schedule.setStatus(LectureStatus.MISSED);
                schedule.setIsActive(false);
                schedule.setAttendanceToken(null);
                schedule.setAttendanceTokenExpiry(null);
            } else if (schedule.getStatus() == LectureStatus.IN_PROGRESS && now.isAfter(scheduleEnd)) {
                schedule.setStatus(LectureStatus.COMPLETED);
                schedule.setIsActive(false);
                schedule.setSessionEndTime(now);
                schedule.setAttendanceToken(null);
                schedule.setAttendanceTokenExpiry(null);
            }
        }

        lectureScheduleRepository.saveAll(schedules);
    }

    @Override
    @Transactional
    public void startLecture(Long id, HttpServletRequest request) {
        Long scheduleId = Objects.requireNonNull(id, "schedule id is required");
        LectureSchedule schedule = lectureScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Lecture schedule not found: " + id));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleStart = LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
        LocalDateTime scheduleEnd = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());

        if (now.isBefore(scheduleStart)) {
            throw new RuntimeException("ლექციის დაწყება შეუძლებელია დაგეგმილ დრომდე");
        }

        if (now.isAfter(scheduleEnd)) {
            schedule.setStatus(LectureStatus.MISSED);
            schedule.setIsActive(false);
            lectureScheduleRepository.save(schedule);
            throw new RuntimeException("ლექციის დრო უკვე გავიდა");
        }

        if (!ipVerificationService.verifyIpAddress(schedule.getRoom().getRoomNumber(), request)) {
            throw new RuntimeException("თქვენ არ იმყოფებით სწორ ოთახში! ლექციის დასაწყებად გადადით ოთახში: " + schedule.getRoom().getRoomNumber());
        }

        schedule.setStatus(LectureStatus.IN_PROGRESS);
        schedule.setIsActive(true);
        schedule.setSessionStartTime(now);

        // Generate attendance token for QR-based student check-in
        String token = UUID.randomUUID().toString();
        schedule.setAttendanceToken(token);
        LocalDateTime tokenExpiry = scheduleEnd.plusMinutes(15);
        schedule.setAttendanceTokenExpiry(tokenExpiry);

        lectureScheduleRepository.save(schedule);

        logger.info("Lecture schedule with ID {} started successfully. Status: {}", id, schedule.getStatus());
    }

    @Override
    @Transactional
    public void stopLecture(Long id, HttpServletRequest request) {
        Long scheduleId = Objects.requireNonNull(id, "schedule id is required");
        LectureSchedule schedule = lectureScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("ლექციის განრიგი ვერ მოიძებნა: " + id));

        if (!ipVerificationService.verifyIpAddress(schedule.getRoom().getRoomNumber(), request)) {
            throw new RuntimeException("თქვენ არ იმყოფებით სწორ ოთახში! ლექციის დასასრულებლად გადადით ოთახში: " + schedule.getRoom().getRoomNumber());
        }

        if (schedule.getStatus() != LectureStatus.IN_PROGRESS) {
            throw new RuntimeException("მხოლოდ მიმდინარე ლექციების დასრულებაა შესაძლებელი");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleEnd = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());

        if (now.isBefore(scheduleEnd)) {
            throw new RuntimeException("ლექციის დასრულება შეუძლებელია დაგეგმილ დრომდე");
        }

        schedule.setStatus(LectureStatus.COMPLETED);
        schedule.setSessionEndTime(now);
        schedule.setIsActive(false);
        schedule.setAttendanceToken(null);
        schedule.setAttendanceTokenExpiry(null);
        lectureScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void saveLecture(LectureDto lectureDto) {
        Lecture lecture = new Lecture();
        List<User> lecturers = resolveLecturers(lectureDto.getLecturerIds());
        lecture.setLecturers(lecturers);
        lecture.setLecturer(lecturers.get(0).getName());
        lecture.setSubject(lectureDto.getSubject());
        lecture.setGroups(resolveGroups(lectureDto.getGroupIds()));

        Lecture savedLecture = lectureRepository.save(lecture);
        addSchedule(savedLecture.getId(), lectureDto);
    }

    @Override
    @Transactional
    public void editLecture(LectureDto lectureDto, Long id) {
        Long lectureId = Objects.requireNonNull(id, "lecture id is required");
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + id));

        List<User> lecturers = resolveLecturers(lectureDto.getLecturerIds());
        lecture.setLecturers(lecturers);
        lecture.setLecturer(lecturers.get(0).getName());
        lecture.setSubject(lectureDto.getSubject());
        lecture.setGroups(resolveGroups(lectureDto.getGroupIds()));
        lectureRepository.save(lecture);

        Long scheduleId = lectureDto.getScheduleId();
        LectureSchedule schedule;
        if (scheduleId != null) {
            schedule = lectureScheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("Lecture schedule not found: " + scheduleId));
            if (!schedule.getLecture().getId().equals(id)) {
                throw new RuntimeException("Schedule does not belong to selected lecture");
            }
        } else {
            List<LectureSchedule> schedules = lectureScheduleRepository.findByLecture_IdOrderByDateAscStartTimeAsc(lectureId);
            if (schedules.isEmpty()) {
                addSchedule(lectureId, lectureDto);
                return;
            }
            schedule = schedules.get(0);
        }

        applyScheduleFields(schedule, lectureDto);
        lectureScheduleRepository.save(Objects.requireNonNull(schedule, "schedule must not be null"));
    }

    @Override
    public LectureDto findLectureById(Long id) {
        Long lectureId = Objects.requireNonNull(id, "lecture id is required");
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + id));
        return mapMasterToDto(lecture);
    }

    @Override
    @Transactional
    public void deleteLectureById(Long id) {
        Long lectureId = Objects.requireNonNull(id, "lecture id is required");
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + id));

        // Delete associated learning resources and their files before deleting the lecture
        List<LearningResource> resources = learningResourceRepository.findByLectureId(lectureId);
        for (LearningResource resource : resources) {
            learningResourceStorageService.delete(resource.getStoredFilename());
            learningResourceRepository.delete(resource);
        }

        lectureRepository.delete(Objects.requireNonNull(lecture, "lecture must not be null"));
    }

    @Override
    @Transactional
    public void addSchedule(Long lectureId, LectureDto scheduleDto) {
        Long nonNullLectureId = Objects.requireNonNull(lectureId, "lecture id is required");
        Lecture lecture = lectureRepository.findById(nonNullLectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture not found"));

        LectureSchedule schedule = new LectureSchedule();
        schedule.setLecture(lecture);
        applyScheduleFields(schedule, scheduleDto);
        lectureScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void deleteScheduleById(Long scheduleId) {
        Long nonNullScheduleId = Objects.requireNonNull(scheduleId, "schedule id is required");
        LectureSchedule schedule = lectureScheduleRepository.findById(nonNullScheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture schedule not found"));
        lectureScheduleRepository.delete(Objects.requireNonNull(schedule, "schedule must not be null"));
    }

    @Override
    public List<LectureDto> findSchedulesByLectureId(Long lectureId) {
        return lectureScheduleRepository.findByLecture_IdOrderByDateAscStartTimeAsc(lectureId).stream()
                .map(this::mapScheduleToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findAllLectures() {
        return lectureRepository.findAllByOrderByIdDesc().stream()
                .map(this::mapMasterToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findLecturesByLecturerId(Long lecturerId) {
        Long nonNullLecturerId = Objects.requireNonNull(lecturerId, "lecturer id is required");
        return lectureRepository.findDistinctByLecturers_IdOrderByIdDesc(nonNullLecturerId).stream()
                .map(this::mapMasterToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findLecturesByDate(LocalDate date) {
        return lectureScheduleRepository.findByDateOrderByStartTimeDesc(date).stream()
                .sorted(Comparator.comparing(LectureSchedule::getStartTime))
                .map(this::mapScheduleToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findLecturesByDateAndLecturerId(LocalDate date, Long lecturerId) {
        Long nonNullLecturerId = Objects.requireNonNull(lecturerId, "lecturer id is required");
        return lectureScheduleRepository.findDistinctByLecture_Lecturers_IdAndDateOrderByStartTimeAsc(nonNullLecturerId, date).stream()
                .map(this::mapScheduleToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findLecturesByGroupId(Long groupId) {
        return lectureRepository.findDistinctByGroups_IdOrderByIdDesc(groupId).stream()
                .map(this::mapMasterToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findLecturesByDateAndGroupId(LocalDate date, Long groupId) {
        return lectureScheduleRepository.findByLecture_Groups_IdAndDateOrderByStartTimeAsc(groupId, date).stream()
                .map(this::mapScheduleToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findUpcomingLecturesByGroupId(Long groupId, LocalDate date) {
        return lectureScheduleRepository.findByLecture_Groups_IdAndDateGreaterThanOrderByDateAscStartTimeAsc(groupId, date).stream()
                .map(this::mapScheduleToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void importLecturesFromCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int lineNumber = 1;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",");
                    if (values.length < 7) {
                        throw new RuntimeException("ფორმატი უნდა იყოს: ოთახი,თარიღი,დაწყება,დასრულება,ლექტორები,საგანი,ჯგუფები");
                    }

                    LectureDto dto = new LectureDto();
                    dto.setRoomNumber(values[0].trim());
                    dto.setDate(LocalDate.parse(values[1].trim()));
                    dto.setStartTime(LocalTime.parse(values[2].trim()));
                    dto.setEndTime(LocalTime.parse(values[3].trim()));
                    String[] lecturerNames = values[4].split(";");
                    List<Long> lecturerIds = resolveLecturerIdsFromNames(lecturerNames);
                    dto.setLecturerIds(lecturerIds);
                    dto.setLecturer(String.join(", ", lecturerNames));
                    dto.setSubject(values[5].trim());
                    dto.setStatus(LectureStatus.SCHEDULED);

                    String[] groupCodes = values[6].split(";");
                    List<Long> groupIds = new ArrayList<>();
                    for (String code : groupCodes) {
                        StudentGroup group = studentGroupRepository.findByCode(code.trim());
                        if (group == null) {
                            throw new RuntimeException("ჯგუფი ვერ მოიძებნა: " + code.trim());
                        }
                        groupIds.add(group.getId());
                    }
                    dto.setGroupIds(groupIds);

                    saveLecture(dto);
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV ფაილის წაკითხვა ვერ მოხერხდა: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("იმპორტი დასრულდა შეცდომებით:\n" + String.join("\n", errors));
        }
    }

    private void applyScheduleFields(LectureSchedule schedule, LectureDto dto) {
        Room room = roomRepository.findByRoomNumber(dto.getRoomNumber());
        if (room == null) {
            throw new RuntimeException("Room not found: " + dto.getRoomNumber());
        }

        schedule.setRoom(room);
        schedule.setDate(dto.getDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());

        LectureStatus resolvedStatus = dto.getStatus() == null ? LectureStatus.SCHEDULED : dto.getStatus();
        if (resolvedStatus == LectureStatus.SCHEDULED) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduleEnd = LocalDateTime.of(dto.getDate(), dto.getEndTime());
            if (now.isAfter(scheduleEnd)) {
                resolvedStatus = LectureStatus.MISSED;
            }
        }
        schedule.setStatus(resolvedStatus);

        if (schedule.getStatus() == LectureStatus.SCHEDULED) {
            schedule.setIsActive(false);
            schedule.setSessionStartTime(null);
            schedule.setSessionEndTime(null);
        }
    }

    private LectureDto mapMasterToDto(Lecture lecture) {
        LectureDto dto = new LectureDto();
        dto.setId(lecture.getId());
        dto.setLectureId(lecture.getId());
        List<User> lecturers = lecture.getLecturers();
        dto.setLecturerIds(lecturers.stream().map(User::getId).collect(Collectors.toList()));
        dto.setLecturerNames(lecturers.stream().map(User::getName).collect(Collectors.toList()));
        if (!dto.getLecturerNames().isEmpty()) {
            dto.setLecturer(String.join(", ", dto.getLecturerNames()));
        } else {
            dto.setLecturer(lecture.getLecturer());
        }
        dto.setSubject(lecture.getSubject());
        dto.setGroupIds(lecture.getGroups().stream().map(StudentGroup::getId).collect(Collectors.toList()));
        dto.setGroupCodes(lecture.getGroups().stream().map(StudentGroup::getCode).collect(Collectors.toList()));

        List<LectureSchedule> schedules = lectureScheduleRepository.findByLecture_IdOrderByDateAscStartTimeAsc(lecture.getId());
        dto.setScheduleCount(schedules.size());
        if (!schedules.isEmpty()) {
            LectureSchedule next = schedules.get(0);
            dto.setScheduleId(next.getId());
            dto.setRoomNumber(next.getRoom().getRoomNumber());
            dto.setDate(next.getDate());
            dto.setStartTime(next.getStartTime());
            dto.setEndTime(next.getEndTime());
            dto.setStatus(next.getStatus());
        }
        return dto;
    }

    private LectureDto mapScheduleToDto(LectureSchedule schedule) {
        LectureDto dto = new LectureDto();
        dto.setId(schedule.getId());
        dto.setScheduleId(schedule.getId());
        dto.setLectureId(schedule.getLecture().getId());
        List<User> lecturers = schedule.getLecture().getLecturers();
        dto.setLecturerIds(lecturers.stream().map(User::getId).collect(Collectors.toList()));
        dto.setLecturerNames(lecturers.stream().map(User::getName).collect(Collectors.toList()));
        if (!dto.getLecturerNames().isEmpty()) {
            dto.setLecturer(String.join(", ", dto.getLecturerNames()));
        } else {
            dto.setLecturer(schedule.getLecture().getLecturer());
        }
        dto.setSubject(schedule.getLecture().getSubject());
        dto.setRoomNumber(schedule.getRoom().getRoomNumber());
        dto.setDate(schedule.getDate());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setStatus(schedule.getStatus());
        dto.setGroupIds(schedule.getLecture().getGroups().stream().map(StudentGroup::getId).collect(Collectors.toList()));
        dto.setGroupCodes(schedule.getLecture().getGroups().stream().map(StudentGroup::getCode).collect(Collectors.toList()));
        dto.setScheduleCount(lectureScheduleRepository.findByLecture_IdOrderByDateAscStartTimeAsc(schedule.getLecture().getId()).size());
        return dto;
    }

    private List<StudentGroup> resolveGroups(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            throw new RuntimeException("ლექციას მინიმუმ ერთი ჯგუფი უნდა ჰქონდეს მინიჭებული");
        }

        Set<Long> uniqueIds = new HashSet<>(groupIds);
        List<StudentGroup> groups = studentGroupRepository.findAllById(uniqueIds);
        if (groups.size() != uniqueIds.size()) {
            throw new RuntimeException("ერთ-ერთი ჯგუფი ვერ მოიძებნა");
        }
        return groups;
    }

    private List<User> resolveLecturers(List<Long> lecturerIds) {
        if (lecturerIds == null || lecturerIds.isEmpty()) {
            throw new RuntimeException("ლექციას მინიმუმ ერთი ლექტორი უნდა ჰქონდეს მინიჭებული");
        }

        Set<Long> uniqueIds = new HashSet<>(lecturerIds);
        List<User> lecturers = userRepository.findAllById(uniqueIds);
        if (lecturers.size() != uniqueIds.size()) {
            throw new RuntimeException("ერთ-ერთი ლექტორი ვერ მოიძებნა");
        }

        boolean hasNonLecturer = lecturers.stream()
                .anyMatch(user -> user.getRoles().stream().noneMatch(role -> "ROLE_LECTURER".equals(role.getName())));
        if (hasNonLecturer) {
            throw new RuntimeException("მონიშნული მომხმარებელი ლექტორი არ არის");
        }

        return lecturers;
    }

    private List<Long> resolveLecturerIdsFromNames(String[] lecturerNames) {
        List<User> allLecturers = userRepository.findAllByRoles_Name("ROLE_LECTURER");
        List<Long> ids = new ArrayList<>();
        for (String rawName : lecturerNames) {
            String name = rawName.trim();
            if (name.isEmpty()) {
                continue;
            }
            User matched = allLecturers.stream()
                    .filter(u -> u.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("ლექტორი ვერ მოიძებნა: " + name));
            ids.add(matched.getId());
        }
        if (ids.isEmpty()) {
            throw new RuntimeException("CSV-ში ლექტორი სავალდებულოა");
        }
        return ids;
    }

    @Override
    public boolean canLecturerManageSchedule(Long scheduleId, Long lecturerId) {
        return lectureScheduleRepository.existsByIdAndLecture_Lecturers_Id(scheduleId, lecturerId);
    }

    @Override
    public boolean canLecturerManageLecture(Long lectureId, Long lecturerId) {
        return lectureRepository.existsByIdAndLecturers_Id(lectureId, lecturerId);
    }
}
