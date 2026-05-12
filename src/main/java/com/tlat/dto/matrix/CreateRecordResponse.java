package com.tlat.dto.matrix;

import lombok.Data;

@Data
public class CreateRecordResponse {
    private boolean success;
    private Long recordId;
    private String letterGrade;
    private double newTotalAttendance;
    private double newTotal;
    private String newTotalGrade;
    private String message;
}