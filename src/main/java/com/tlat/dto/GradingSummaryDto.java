package com.tlat.dto;

import lombok.Data;

@Data
public class GradingSummaryDto {
    private Long studentId;
    private String studentName;
    private String subject;
    private Long groupId;
    private Double attendanceScore = 0.0;
    private Double midtermScore = 0.0;
    private Double finalScore = 0.0;
    
    public Double getTotalScore() {
        return (attendanceScore != null ? attendanceScore : 0.0) +
               (midtermScore != null ? midtermScore : 0.0) +
               (finalScore != null ? finalScore : 0.0);
    }
    
    public String getLetterGrade() {
        double total = getTotalScore();
        if (total >= 91) return "A";
        if (total >= 81) return "B";
        if (total >= 71) return "C";
        if (total >= 61) return "D";
        if (total >= 51) return "E";
        if (total >= 41) return "FX";
        return "F";
    }
}
