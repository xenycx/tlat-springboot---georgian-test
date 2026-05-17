package com.tlat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lecture_schedules", indexes = {
        @Index(name = "idx_schedule_lecture", columnList = "lecture_id"),
        @Index(name = "idx_schedule_date", columnList = "date")
})
public class LectureSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Boolean isActive = false;

    @Column
    private LocalDateTime sessionStartTime;

    @Column
    private LocalDateTime sessionEndTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LectureStatus status = LectureStatus.SCHEDULED;

    @Column(name = "attendance_token")
    private String attendanceToken;

    @Column(name = "attendance_token_expiry")
    private LocalDateTime attendanceTokenExpiry;
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AttendanceRecord> attendanceRecords = new ArrayList<>();
}
