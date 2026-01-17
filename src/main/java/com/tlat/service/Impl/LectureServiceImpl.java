package com.tlat.service.Impl;

import com.tlat.Dto.LectureDto;
import com.tlat.Entity.Lecture;
import com.tlat.Entity.LectureStatus;
import com.tlat.Entity.Room;
import com.tlat.Repository.LectureRepository;
import com.tlat.Repository.RoomRepository;
import com.tlat.service.IpVerificationService;
import com.tlat.service.LectureService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LectureServiceImpl implements LectureService {

    private final LectureRepository lectureRepository;
    private final RoomRepository roomRepository;
    private final IpVerificationService ipVerificationService;

    private static final Logger logger = LoggerFactory.getLogger(LectureServiceImpl.class);

    @Autowired
    public LectureServiceImpl(LectureRepository lectureRepository, RoomRepository roomRepository, IpVerificationService ipVerificationService) {
        this.lectureRepository = lectureRepository;
        this.roomRepository = roomRepository;
        this.ipVerificationService = ipVerificationService;
    }

    @Scheduled(fixedRate = 60000) // ეშვება ყოველ 1 წუთში ერთხელ
    public void updateLectureStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Lecture> lectures = lectureRepository.findAll();

        for (Lecture lecture : lectures) {
            LocalDateTime lectureEnd = LocalDateTime.of(lecture.getDate(), lecture.getEndTime());

            // მხოლოდ ანახლებს მაშინ როცა სტატუსია დაგეგმილი
            if (lecture.getStatus() == LectureStatus.SCHEDULED) {
                // თუ მიმდინარე დრო ლექციის დასრულების დროზე მეტია და ლექცია არ დაწყებულა
                if (now.isAfter(lectureEnd)) {
                    lecture.setStatus(LectureStatus.MISSED);
                    lecture.setIsActive(false);
                }
            }
            // თუ ლექცია მიმდინარეობს და მიმდინარე დრო ლექციის დასრულების დროზე მეტია
            else if (lecture.getStatus() == LectureStatus.IN_PROGRESS && now.isAfter(lectureEnd)) {
                lecture.setStatus(LectureStatus.COMPLETED);
                lecture.setIsActive(false);
                lecture.setSessionEndTime(now);
            }
        }

        lectureRepository.saveAll(lectures);
    }

    @Override
    public void startLecture(Long id, HttpServletRequest request) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + id));

        // Verify time constraints
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lectureStart = LocalDateTime.of(lecture.getDate(), lecture.getStartTime());
        LocalDateTime lectureEnd = LocalDateTime.of(lecture.getDate(), lecture.getEndTime());

        // Check if lecture is scheduled for current time
        if (now.isBefore(lectureStart)) {
            throw new RuntimeException("ლექციის დაწყება შეუძლებელია დაგეგმილ დრომდე");
        }

        if (now.isAfter(lectureEnd)) {
            lecture.setStatus(LectureStatus.MISSED);
            lecture.setIsActive(false);
            lectureRepository.save(lecture);
            throw new RuntimeException("ლექციის დრო უკვე გავიდა");
        }

        // Verify IP address
        if (!ipVerificationService.verifyIpAddress(lecture.getRoom().getRoomNumber(), request)) {
            throw new RuntimeException("თქვენ არ იმყოფებით სწორ ოთახში! ლექციის დასაწყებად გადადით ოთახში: " + lecture.getRoom().getRoomNumber());
        }

        // Update lecture status
        lecture.setStatus(LectureStatus.IN_PROGRESS);
        lecture.setIsActive(true);
        lecture.setSessionStartTime(now);
        lectureRepository.save(lecture);

        logger.info("Lecture with ID {} started successfully. Status: {}", id, lecture.getStatus());
    }

    @Override
    public void stopLecture(Long id, HttpServletRequest request) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ლექცია ვერ მოიძებნა: " + id));

        // Verify IP address
        if (!ipVerificationService.verifyIpAddress(lecture.getRoom().getRoomNumber(), request)) {
            throw new RuntimeException("თქვენ არ იმყოფებით სწორ ოთახში! ლექციის დასასრულებლად გადადით ოთახში: " + lecture.getRoom().getRoomNumber());
        }

        // Verify lecture status
        if (lecture.getStatus() != LectureStatus.IN_PROGRESS) {
            throw new RuntimeException("მხოლოდ მიმდინარე ლექციების დასრულებაა შესაძლებელი");
        }

        // Verify time constraints
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lectureEnd = LocalDateTime.of(lecture.getDate(), lecture.getEndTime());

        if (now.isBefore(lectureEnd)) {
            throw new RuntimeException("ლექციის დასრულება შეუძლებელია დაგეგმილ დრომდე");
        }

        // Update lecture status
        lecture.setStatus(LectureStatus.COMPLETED);
        lecture.setSessionEndTime(now);
        lecture.setIsActive(false);
        lectureRepository.save(lecture);
    }

    @Override
    public void saveLecture(LectureDto lectureDto) {
        Lecture lecture = mapToEntity(lectureDto);
        // Always set initial status to SCHEDULED
        lecture.setStatus(LectureStatus.SCHEDULED);
        lecture.setIsActive(false);
        lectureRepository.save(lecture);
    }

    @Override
    public void editLecture(LectureDto lectureDto, Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + id));
        lecture.setDate(lectureDto.getDate());
        lecture.setStartTime(lectureDto.getStartTime());
        lecture.setEndTime(lectureDto.getEndTime());
        lecture.setLecturer(lectureDto.getLecturer());
        lecture.setSubject(lectureDto.getSubject());
        
        // Update status from form - allow rescheduling lectures
        if (lectureDto.getStatus() != null) {
            lecture.setStatus(lectureDto.getStatus());
            // Reset active flag based on status
            if (lectureDto.getStatus() == LectureStatus.SCHEDULED) {
                lecture.setIsActive(false);
                lecture.setSessionStartTime(null);
                lecture.setSessionEndTime(null);
            }
        }
        
        Room room = roomRepository.findByRoomNumber(lectureDto.getRoomNumber());
        if (room == null) {
            throw new RuntimeException("Room not found: " + lectureDto.getRoomNumber());
        }
        lecture.setRoom(room);
        lectureRepository.save(lecture);
    }

    @Override
    public LectureDto findLectureById(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + id));
        return mapToDto(lecture);
    }

    @Override
    public void deleteLectureById(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + id));
        lectureRepository.delete(lecture);
    }

    @Override
    public List<LectureDto> findAllLectures() {
        List<Lecture> lectures = lectureRepository.findAllByOrderByIdDesc();
        return lectures.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void importLecturesFromCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int lineNumber = 1; // სათაურის არ გათვალისწინებისთვის

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // სათაურის არ გათვალისწინებისთვის
            reader.readLine();
            
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",");
                    String roomNumber = values[0].trim();
                    
                    // დავრწმუნდეთ, რომ ოთახი არსებობს
                    Room room = roomRepository.findByRoomNumber(roomNumber);
                    if (room == null) {
                        errors.add("Line " + lineNumber + ": ოთახი არ მოიძებნა: " + roomNumber);
                        continue;
                    }

                    LectureDto lectureDto = new LectureDto();
                    lectureDto.setRoomNumber(roomNumber);
                    lectureDto.setDate(LocalDate.parse(values[1].trim()));
                    lectureDto.setStartTime(LocalTime.parse(values[2].trim()));
                    lectureDto.setEndTime(LocalTime.parse(values[3].trim()));
                    lectureDto.setLecturer(values[4].trim());
                    lectureDto.setSubject(values[5].trim());
                    
                    saveLecture(lectureDto);
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

    @Override
    public List<LectureDto> findLecturesByLecturer(String lecturer) {
        List<Lecture> lectures = lectureRepository.findByLecturerOrderByIdDesc(lecturer);
        return lectures.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findLecturesByDate(LocalDate date) {
        List<Lecture> lectures = lectureRepository.findByDate(date);
        return lectures.stream()
                .map(this::mapToDto)
                .sorted(Comparator.comparing(LectureDto::getStartTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<LectureDto> findLecturesByDateAndLecturer(LocalDate date, String lecturer) {
        return lectureRepository.findByDateAndLecturer(date, lecturer)
                .stream()
                .map(this::mapToDto)
                .sorted(Comparator.comparing(LectureDto::getStartTime))
                .collect(Collectors.toList());
    }

    private Lecture mapToEntity(LectureDto dto) {
        Lecture lecture = new Lecture();
        Room room = roomRepository.findByRoomNumber(dto.getRoomNumber());
        if (room == null) {
            throw new RuntimeException("Room not found: " + dto.getRoomNumber());
        }
        
        lecture.setRoom(room);
        lecture.setDate(dto.getDate());
        lecture.setStartTime(dto.getStartTime());
        lecture.setEndTime(dto.getEndTime());
        lecture.setLecturer(dto.getLecturer());
        lecture.setSubject(dto.getSubject());
        lecture.setIsActive(false);
        lecture.setStatus(dto.getStatus());
        return lecture;
    }

    private LectureDto mapToDto(Lecture lecture) {
        LectureDto dto = new LectureDto();
        dto.setId(lecture.getId());
        dto.setRoomNumber(lecture.getRoom().getRoomNumber());
        dto.setDate(lecture.getDate());
        dto.setStartTime(lecture.getStartTime());
        dto.setEndTime(lecture.getEndTime());
        dto.setLecturer(lecture.getLecturer());
        dto.setSubject(lecture.getSubject());
        dto.setStatus(lecture.getStatus());
        
        return dto;
    }
}