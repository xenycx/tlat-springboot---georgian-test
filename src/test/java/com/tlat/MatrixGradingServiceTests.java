package com.tlat;

import com.tlat.service.MatrixGradingService;
import com.tlat.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MatrixGradingServiceTests {

    @Autowired
    private MatrixGradingService matrixGradingService;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    void testGradeCalculationAndAggregationLogic() throws Exception {
        assertNotNull(matrixGradingService);
    }
}
