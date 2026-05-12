package com.tlat.dto.matrix;

import lombok.Data;

@Data
public class UpdateExamRequest {
    private Long studentId;
    private Long lectureId;
    private Long groupId;
    private String examType; // "MIDTERM" or "FINAL"
    private Double score;
}