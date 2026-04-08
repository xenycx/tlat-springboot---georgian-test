package com.tlat.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

import com.tlat.Entity.LectureStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LectureDto {
    private Long id;

    private Long lectureId;

    private Long scheduleId;
    
    @NotBlank(message = "Room number is required")
    private String roomNumber;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    private String lecturer;

    private List<Long> lecturerIds = new ArrayList<>();

    private List<String> lecturerNames = new ArrayList<>();
    
    @NotBlank(message = "Subject is required")
    private String subject;

    @NotNull(message = "Status is required")
    private LectureStatus status = LectureStatus.SCHEDULED;

    private List<Long> groupIds = new ArrayList<>();

    private List<String> groupCodes = new ArrayList<>();

    private Integer scheduleCount = 0;

    public LectureStatus getStatus() {
        return status != null ? status : LectureStatus.SCHEDULED;
    }

    public void setStatus(LectureStatus status) {
        this.status = status;
    }
}
