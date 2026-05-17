package com.tlat.repository;

import com.tlat.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("SELECT COALESCE(SUM(a.score), 0) FROM AttendanceRecord a JOIN a.schedule s JOIN s.lecture l JOIN l.groups g WHERE a.student.id = :studentId AND l.subject = :subject AND g.id = :groupId")
    Double sumScoreByStudentIdAndSubjectAndGroupId(@Param("studentId") Long studentId, @Param("subject") String subject, @Param("groupId") Long groupId);

    List<AttendanceRecord> findBySchedule_IdOrderByAttendedAtAsc(Long scheduleId);

    boolean existsBySchedule_IdAndStudent_Id(Long scheduleId, Long studentId);

    Optional<AttendanceRecord> findBySchedule_IdAndStudent_Id(Long scheduleId, Long studentId);

    long countBySchedule_Id(Long scheduleId);

    List<AttendanceRecord> findBySchedule_Lecture_Id(Long lectureId);
}
