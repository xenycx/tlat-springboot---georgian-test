package com.tlat.controller;

import com.tlat.dto.matrix.UpdateExamRequest;
import com.tlat.dto.matrix.CreateRecordRequest;
import com.tlat.dto.matrix.CreateRecordResponse;
import com.tlat.dto.matrix.MatrixGradingDTO;
import com.tlat.dto.matrix.UpdateScoreRequest;
import com.tlat.dto.matrix.UpdateScoreResponse;
import com.tlat.service.MatrixGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/grading/matrix")
@RequiredArgsConstructor
public class MatrixGradingRestController {

    private final MatrixGradingService matrixGradingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<MatrixGradingDTO> getMatrixData(
            @RequestParam Long lectureId,
            @RequestParam(required = false) Long groupId,
            Principal principal) {
        MatrixGradingDTO dto = matrixGradingService.getMatrixData(lectureId, groupId, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/update-score")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<UpdateScoreResponse> updateScore(
            @RequestBody UpdateScoreRequest request,
            Principal principal) {
        UpdateScoreResponse response = matrixGradingService.updateScore(request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-record")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<CreateRecordResponse> createRecord(
            @RequestBody CreateRecordRequest request,
            Principal principal) {
        CreateRecordResponse response = matrixGradingService.createRecord(request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-exam")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<UpdateScoreResponse> updateExamScore(
            @RequestBody UpdateExamRequest request,
            Principal principal) {
        UpdateScoreResponse response = matrixGradingService.updateExamScore(request, principal.getName());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler({IllegalArgumentException.class, SecurityException.class})
    public ResponseEntity<UpdateScoreResponse> handleExceptions(RuntimeException ex) {
        UpdateScoreResponse response = new UpdateScoreResponse();
        response.setSuccess(false);
        response.setMessage(ex.getMessage());
        return ResponseEntity.ok(response);
    }
}