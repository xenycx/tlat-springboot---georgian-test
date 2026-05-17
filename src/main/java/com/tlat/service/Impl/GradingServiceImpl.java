package com.tlat.service.Impl;

import com.tlat.dto.AttendanceBatchDto;
import com.tlat.dto.GradingSummaryDto;
import com.tlat.entity.*;
import com.tlat.repository.*;
import com.tlat.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LectureScheduleRepository scheduleRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;

    @Override
    @Transactional
    public void saveBatchAttendance(AttendanceBatchDto batchDto, Long currentUserId) {
        Long scheduleId = Objects.requireNonNull(batchDto.getScheduleId(), "scheduleId is required");
        LectureSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid schedule ID"));

        User addedBy = null;
        if (currentUserId != null) {
            addedBy = userRepository.findById(currentUserId).orElse(null);
        }

        for (AttendanceBatchDto.StudentAttendanceDto dto : batchDto.getStudents()) {
            Long studentId = Objects.requireNonNull(dto.getStudentId(), "studentId is required");
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid student ID: " + dto.getStudentId()));

            Optional<AttendanceRecord> existingRecordOpt = attendanceRecordRepository
                    .findBySchedule_IdAndStudent_Id(schedule.getId(), student.getId());

            AttendanceRecord record;
            if (existingRecordOpt.isPresent()) {
                record = existingRecordOpt.get();
                record.setStatus(dto.getStatus());
                record.setScore(dto.getScore() != null ? dto.getScore() : 0.0);
                record.setNote(dto.getNote());
                if (addedBy != null) {
                    record.setAddedBy(addedBy);
                }
            } else {
                record = new AttendanceRecord();
                record.setSchedule(schedule);
                record.setStudent(student);
                record.setCaptureMethod(AttendanceCaptureMethod.MANUAL);
                record.setAttendedAt(LocalDateTime.now());
                record.setVerified(true);
                record.setStatus(dto.getStatus() != null ? dto.getStatus() : AttendanceStatus.PRESENT);
                record.setScore(dto.getScore() != null ? dto.getScore() : 0.0);
                record.setNote(dto.getNote());
                record.setAddedBy(addedBy);
            }
            attendanceRecordRepository.save(record);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradingSummaryDto> getGradesForSubjectAndGroup(String subject, Long groupId) {
        Long nonNullGroupId = Objects.requireNonNull(groupId, "groupId is required");
        StudentGroup group = studentGroupRepository.findById(nonNullGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid group ID"));

        // Get all students in this group
        List<User> students = group.getStudents();
        if (students == null || students.isEmpty()) {
            return Collections.emptyList();
        }

        // Get all student grades for this subject and group
        List<StudentGrade> grades = studentGradeRepository.findBySubjectAndGroupId(subject, nonNullGroupId);
        Map<Long, StudentGrade> gradeMap = grades.stream()
                .collect(Collectors.toMap(g -> g.getStudent().getId(), g -> g));

        // Get all attendance records for this subject and group
        // Calculate attendance score by querying per student to get sum of scores.
        
        // We construct the result
        List<GradingSummaryDto> summaries = new ArrayList<>();
        for (User student : students) {
            GradingSummaryDto dto = new GradingSummaryDto();
            dto.setStudentId(student.getId());
            dto.setStudentName(student.getName());
            dto.setSubject(subject);
            dto.setGroupId(nonNullGroupId);

            StudentGrade grade = gradeMap.get(student.getId());
            if (grade != null) {
                dto.setMidtermScore(grade.getMidtermScore());
                dto.setFinalScore(grade.getFinalScore());
            }

            // Calculate attendance score. A simple query per student to get sum of scores:
            Double attScore = attendanceRecordRepository.sumScoreByStudentIdAndSubjectAndGroupId(student.getId(), subject, nonNullGroupId);
            dto.setAttendanceScore(attScore != null ? attScore : 0.0);

            summaries.add(dto);
        }
        return summaries;
    }

    @Override
    @Transactional
    public void saveMidtermAndFinalScores(String subject, Long groupId, List<GradingSummaryDto> dtos) {
        Long nonNullGroupId = Objects.requireNonNull(groupId, "groupId is required");
        StudentGroup group = studentGroupRepository.findById(nonNullGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid group ID"));

        for (GradingSummaryDto dto : dtos) {
            Long studentId = Objects.requireNonNull(dto.getStudentId(), "studentId is required");
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid student ID: " + dto.getStudentId()));

            Optional<StudentGrade> existingGrade = studentGradeRepository
                .findByStudentIdAndSubjectAndGroupId(student.getId(), subject, nonNullGroupId);

            StudentGrade grade;
            if (existingGrade.isPresent()) {
                grade = existingGrade.get();
                grade.setMidtermScore(dto.getMidtermScore() != null ? dto.getMidtermScore() : 0.0);
                grade.setFinalScore(dto.getFinalScore() != null ? dto.getFinalScore() : 0.0);
            } else {
                grade = new StudentGrade();
                grade.setStudent(student);
                grade.setSubject(subject);
                grade.setGroup(group);
                grade.setMidtermScore(dto.getMidtermScore() != null ? dto.getMidtermScore() : 0.0);
                grade.setFinalScore(dto.getFinalScore() != null ? dto.getFinalScore() : 0.0);
            }
            studentGradeRepository.save(grade);
        }
    }
}
