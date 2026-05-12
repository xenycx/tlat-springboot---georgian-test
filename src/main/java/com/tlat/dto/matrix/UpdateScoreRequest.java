package com.tlat.dto.matrix;

import lombok.Data;

@Data
public class UpdateScoreRequest {
    private Long studentId;
    private Long lectureScheduleId;
    private Double score;
}
