package com.tlat.service;

import com.tlat.dto.matrix.UpdateExamRequest;
import com.tlat.dto.matrix.CreateRecordRequest;
import com.tlat.dto.matrix.CreateRecordResponse;
import com.tlat.dto.matrix.MatrixGradingDTO;
import com.tlat.dto.matrix.UpdateScoreRequest;
import com.tlat.dto.matrix.UpdateScoreResponse;

public interface MatrixGradingService {
    MatrixGradingDTO getMatrixData(Long lectureId, Long groupId, String username);
    UpdateScoreResponse updateScore(UpdateScoreRequest request, String username);
    CreateRecordResponse createRecord(CreateRecordRequest request, String username);
    UpdateScoreResponse updateExamScore(UpdateExamRequest request, String username);
}