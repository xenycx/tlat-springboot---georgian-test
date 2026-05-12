package com.tlat.dto.matrix;

import lombok.Data;

@Data
public class CreateRecordRequest {
    private Long studentId;
    private Long lectureId;
    private Long groupId;
    private Integer week;
    private Double score;
}