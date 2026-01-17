package com.tlat.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="lectures")
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
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
    private String lecturer;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LectureStatus status = LectureStatus.SCHEDULED;
}