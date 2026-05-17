package com.tlat.service.Impl;

import com.tlat.entity.*;
import com.tlat.repository.AttendanceRecordRepository;
import com.tlat.repository.LectureScheduleRepository;
import com.tlat.repository.UserRepository;
import com.tlat.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LectureScheduleRepository lectureScheduleRepository;
    private final UserRepository userRepository;

    @Autowired
    public AttendanceServiceImpl(AttendanceRecordRepository attendanceRecordRepository,
                                  LectureScheduleRepository lectureScheduleRepository,
                                  UserRepository userRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.lectureScheduleRepository = lectureScheduleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public String generateAttendanceToken(Long scheduleId) {
        Long nonNullScheduleId = Objects.requireNonNull(scheduleId, "scheduleId is required");
        LectureSchedule schedule = lectureScheduleRepository.findById(nonNullScheduleId)
                .orElseThrow(() -> new RuntimeException("განრიგი ვერ მოიძებნა"));

        String token = UUID.randomUUID().toString();
        schedule.setAttendanceToken(token);
        LocalDateTime expiry = LocalDateTime.of(schedule.getDate(), schedule.getEndTime()).plusMinutes(15);
        schedule.setAttendanceTokenExpiry(expiry);
        lectureScheduleRepository.save(schedule);

        logger.info("Attendance token generated for schedule {}", scheduleId);
        return token;
    }

    @Override
    @Transactional
    public void invalidateAttendanceToken(Long scheduleId) {
        Long nonNullScheduleId = Objects.requireNonNull(scheduleId, "scheduleId is required");
        LectureSchedule schedule = lectureScheduleRepository.findById(nonNullScheduleId)
                .orElseThrow(() -> new RuntimeException("განრიგი ვერ მოიძებნა"));
        schedule.setAttendanceToken(null);
        schedule.setAttendanceTokenExpiry(null);
        lectureScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public AttendanceRecord markAttendanceAuthenticated(String token, User student, HttpServletRequest request) {
        LectureSchedule schedule = validateAndGetSchedule(token);
        validateStudentEligibility(schedule, student);
        checkDuplicate(schedule.getId(), student.getId());

        AttendanceRecord record = new AttendanceRecord();
        record.setSchedule(schedule);
        record.setStudent(student);
        record.setCaptureMethod(AttendanceCaptureMethod.QR_AUTHENTICATED);
        record.setVerified(true);
        record.setIpAddress(getClientIp(request));
        record.setUserAgent(truncate(request.getHeader("User-Agent"), 500));

        logger.info("Authenticated attendance marked: student={}, schedule={}", student.getId(), schedule.getId());
        return attendanceRecordRepository.save(record);
    }

    @Override
    @Transactional
    public AttendanceRecord markAttendanceSelfIdentified(String token, Long studentId, HttpServletRequest request) {
        LectureSchedule schedule = validateAndGetSchedule(token);
        Long nonNullStudentId = Objects.requireNonNull(studentId, "studentId is required");
        User student = userRepository.findById(nonNullStudentId)
                .orElseThrow(() -> new RuntimeException("სტუდენტი ვერ მოიძებნა"));
        validateStudentEligibility(schedule, student);
        checkDuplicate(schedule.getId(), student.getId());

        AttendanceRecord record = new AttendanceRecord();
        record.setSchedule(schedule);
        record.setStudent(student);
        record.setCaptureMethod(AttendanceCaptureMethod.QR_SELF_IDENTIFIED);
        record.setVerified(false);
        record.setIpAddress(getClientIp(request));
        record.setUserAgent(truncate(request.getHeader("User-Agent"), 500));

        logger.info("Self-identified attendance marked: student={}, schedule={}, ip={}", studentId, schedule.getId(), getClientIp(request));
        return attendanceRecordRepository.save(record);
    }

    @Override
    @Transactional
    public AttendanceRecord markAttendanceManual(Long scheduleId, Long studentId, User lecturer) {
        Long nonNullScheduleId = Objects.requireNonNull(scheduleId, "scheduleId is required");
        Long nonNullStudentId = Objects.requireNonNull(studentId, "studentId is required");
        LectureSchedule schedule = lectureScheduleRepository.findById(nonNullScheduleId)
                .orElseThrow(() -> new RuntimeException("განრიგი ვერ მოიძებნა"));
        User student = userRepository.findById(nonNullStudentId)
                .orElseThrow(() -> new RuntimeException("სტუდენტი ვერ მოიძებნა"));
        validateStudentEligibility(schedule, student);

        if (attendanceRecordRepository.existsBySchedule_IdAndStudent_Id(nonNullScheduleId, nonNullStudentId)) {
            return attendanceRecordRepository.findBySchedule_IdAndStudent_Id(nonNullScheduleId, nonNullStudentId).orElseThrow();
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setSchedule(schedule);
        record.setStudent(student);
        record.setCaptureMethod(AttendanceCaptureMethod.MANUAL);
        record.setVerified(true);
        record.setAddedBy(lecturer);

        logger.info("Manual attendance added: student={}, schedule={}, by={}", studentId, scheduleId, lecturer.getId());
        return attendanceRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void verifyAttendance(Long recordId) {
        Long nonNullRecordId = Objects.requireNonNull(recordId, "recordId is required");
        AttendanceRecord record = attendanceRecordRepository.findById(nonNullRecordId)
                .orElseThrow(() -> new RuntimeException("ჩანაწერი ვერ მოიძებნა"));
        record.setVerified(true);
        attendanceRecordRepository.save(record);
        logger.info("Attendance verified: record={}", recordId);
    }

    @Override
    public List<AttendanceRecord> getAttendanceForSchedule(Long scheduleId) {
        return attendanceRecordRepository.findBySchedule_IdOrderByAttendedAtAsc(
                Objects.requireNonNull(scheduleId, "scheduleId is required"));
    }

    @Override
    public List<User> getEligibleStudentsForSchedule(Long scheduleId) {
        Long nonNullScheduleId = Objects.requireNonNull(scheduleId, "scheduleId is required");
        LectureSchedule schedule = lectureScheduleRepository.findById(nonNullScheduleId)
                .orElseThrow(() -> new RuntimeException("განრიგი ვერ მოიძებნა"));

        List<StudentGroup> groups = schedule.getLecture().getGroups();
        return groups.stream()
                .flatMap(g -> g.getStudents().stream())
                .distinct()
                .sorted(Comparator.comparing(User::getName))
                .collect(Collectors.toList());
    }

    @Override
    public long getAttendanceCount(Long scheduleId) {
        return attendanceRecordRepository.countBySchedule_Id(Objects.requireNonNull(scheduleId, "scheduleId is required"));
    }

    @Override
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) return false;
        Optional<LectureSchedule> opt = lectureScheduleRepository.findByAttendanceToken(token);
        if (opt.isEmpty()) return false;
        LectureSchedule schedule = opt.get();
        return schedule.getStatus() == LectureStatus.IN_PROGRESS
                && schedule.getAttendanceTokenExpiry() != null
                && LocalDateTime.now().isBefore(schedule.getAttendanceTokenExpiry());
    }

    @Override
    public LectureSchedule getScheduleByToken(String token) {
        if (token == null || token.isBlank()) return null;
        return lectureScheduleRepository.findByAttendanceToken(token).orElse(null);
    }

    private LectureSchedule validateAndGetSchedule(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("არასწორი ტოკენი");
        }
        LectureSchedule schedule = lectureScheduleRepository.findByAttendanceToken(token)
                .orElseThrow(() -> new RuntimeException("არასწორი ან ვადაგასული ტოკენი"));

        if (schedule.getStatus() != LectureStatus.IN_PROGRESS) {
            throw new RuntimeException("ლექცია არ არის მიმდინარე");
        }
        if (schedule.getAttendanceTokenExpiry() != null
                && LocalDateTime.now().isAfter(schedule.getAttendanceTokenExpiry())) {
            throw new RuntimeException("ტოკენის ვადა ამოიწურა");
        }
        return schedule;
    }

    private void validateStudentEligibility(LectureSchedule schedule, User student) {
        boolean isStudent = student.getRoles().stream()
                .anyMatch(r -> "ROLE_STUDENT".equals(r.getName()));
        if (!isStudent) {
            throw new RuntimeException("მხოლოდ სტუდენტებს შეუძლიათ დასწრების აღნიშვნა");
        }

        StudentGroup studentGroup = student.getStudentGroup();
        if (studentGroup == null) {
            throw new RuntimeException("სტუდენტი არ არის ჯგუფში");
        }

        boolean inLectureGroup = schedule.getLecture().getGroups().stream()
                .anyMatch(g -> g.getId().equals(studentGroup.getId()));
        if (!inLectureGroup) {
            throw new RuntimeException("თქვენ არ ხართ ამ ლექციის ჯგუფში");
        }
    }

    private void checkDuplicate(Long scheduleId, Long studentId) {
        if (attendanceRecordRepository.existsBySchedule_IdAndStudent_Id(scheduleId, studentId)) {
            throw new RuntimeException("დასწრება უკვე აღნიშნულია");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
