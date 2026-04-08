package com.tlat.service;

import com.tlat.Entity.AttendanceRecord;
import com.tlat.Entity.LectureSchedule;
import com.tlat.Entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AttendanceService {

    String generateAttendanceToken(Long scheduleId);

    void invalidateAttendanceToken(Long scheduleId);

    AttendanceRecord markAttendanceAuthenticated(String token, User student, HttpServletRequest request);

    AttendanceRecord markAttendanceSelfIdentified(String token, Long studentId, HttpServletRequest request);

    AttendanceRecord markAttendanceManual(Long scheduleId, Long studentId, User lecturer);

    List<AttendanceRecord> getAttendanceForSchedule(Long scheduleId);

    List<User> getEligibleStudentsForSchedule(Long scheduleId);

    long getAttendanceCount(Long scheduleId);

    boolean isTokenValid(String token);

    LectureSchedule getScheduleByToken(String token);
}
