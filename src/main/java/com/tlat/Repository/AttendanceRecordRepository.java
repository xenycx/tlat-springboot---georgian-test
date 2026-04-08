package com.tlat.Repository;

import com.tlat.Entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySchedule_IdOrderByAttendedAtAsc(Long scheduleId);

    boolean existsBySchedule_IdAndStudent_Id(Long scheduleId, Long studentId);

    Optional<AttendanceRecord> findBySchedule_IdAndStudent_Id(Long scheduleId, Long studentId);

    long countBySchedule_Id(Long scheduleId);
}
