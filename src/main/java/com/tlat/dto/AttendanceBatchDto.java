package com.tlat.dto;

import com.tlat.entity.AttendanceStatus;
import java.util.List;

public class AttendanceBatchDto {
    private Long scheduleId;
    private List<StudentAttendanceDto> students;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public List<StudentAttendanceDto> getStudents() { return students; }
    public void setStudents(List<StudentAttendanceDto> students) { this.students = students; }

    public static class StudentAttendanceDto {
        private Long studentId;
        private AttendanceStatus status;
        private Double score;
        private String note;
        
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public AttendanceStatus getStatus() { return status; }
        public void setStatus(AttendanceStatus status) { this.status = status; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
