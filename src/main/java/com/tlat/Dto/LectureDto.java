package com.tlat.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

import com.tlat.Entity.LectureStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LectureDto {
    private Long id;
    
    @NotBlank(message = "Room number is required")
    private String roomNumber;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    @NotBlank(message = "Lecturer name is required")
    private String lecturer;
    
    @NotBlank(message = "Subject is required")
    private String subject;

    @NotNull(message = "Status is required")
    private LectureStatus status = LectureStatus.SCHEDULED;

    public LectureStatus getStatus() {
        return status != null ? status : LectureStatus.SCHEDULED;
    }

    public void setStatus(LectureStatus status) {
        this.status = status;
    }
}