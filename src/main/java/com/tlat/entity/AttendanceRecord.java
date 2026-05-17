package com.tlat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_records",
    uniqueConstraints = @UniqueConstraint(name = "uk_schedule_student", columnNames = {"schedule_id", "student_id"}),
    indexes = {
        @Index(name = "idx_attendance_schedule", columnList = "schedule_id"),
        @Index(name = "idx_attendance_student", columnList = "student_id"),
        @Index(name = "idx_attendance_time", columnList = "attended_at")
    })
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private LectureSchedule schedule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceCaptureMethod captureMethod;

    @Column(name = "attended_at", nullable = false)
    private LocalDateTime attendedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false)
    private Boolean verified = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "score")
    private Double score = 0.0;

    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private User addedBy;

    @PrePersist
    protected void onCreate() {
        if (this.attendedAt == null) {
            this.attendedAt = LocalDateTime.now();
        }
    }
}
