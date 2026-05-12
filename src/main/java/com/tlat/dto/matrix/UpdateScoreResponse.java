package com.tlat.dto.matrix;

import lombok.Data;

@Data
public class UpdateScoreResponse {
    private boolean success;
    private String letterGrade; // For week updates
    private double newTotalAttendance;
    private double newTotal;
    private String newTotalGrade;
    private String message;
}
