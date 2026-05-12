package com.tlat.service;

import com.tlat.dto.AttendanceBatchDto;
import com.tlat.dto.GradingSummaryDto;
import java.util.List;

public interface GradingService {
    void saveBatchAttendance(AttendanceBatchDto batchDto, Long currentUserId);
    List<GradingSummaryDto> getGradesForSubjectAndGroup(String subject, Long groupId);
    void saveMidtermAndFinalScores(String subject, Long groupId, List<GradingSummaryDto> grades);
}
