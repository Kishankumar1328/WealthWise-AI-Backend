package ai.wealthwise.service;

import ai.wealthwise.model.entity.ScenarioAnalysis;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.repository.ScenarioAnalysisRepository;
import ai.wealthwise.repository.SmeBusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioAnalysisServiceTest {

    @Mock
    private ScenarioAnalysisRepository scenarioRepository;

    @Mock
    private SmeBusinessRepository businessRepository;

    @InjectMocks
    private ScenarioAnalysisService scenarioService;

    private SmeBusiness business;

    @BeforeEach
    void setUp() {
        business = new SmeBusiness();
        business.setId(1L);
        business.setBusinessName("Test Corp");
    }

    @Test
    void createScenario_Success() {
        when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
        when(scenarioRepository.save(any(ScenarioAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioAnalysis result = scenarioService.createScenario(1L, "Test Scenario", "Description",
                BigDecimal.valueOf(10), BigDecimal.valueOf(5));

        assertNotNull(result);
        assertEquals("Test Scenario", result.getScenarioName());
        assertEquals("Description", result.getDescription());
        assertEquals(business, result.getBusiness());
        assertNotNull(result.getResultSummaryJson());
        assertTrue(result.getResultSummaryJson().contains("projected_revenue"));

        verify(businessRepository).findById(1L);
        verify(scenarioRepository).save(any(ScenarioAnalysis.class));
    }

    @Test
    void createScenario_BusinessNotFound() {
        when(businessRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> scenarioService.createScenario(1L, "Test", "Desc", BigDecimal.TEN, BigDecimal.TEN));
    }

    @Test
    void getScenarios_Success() {
        ScenarioAnalysis s1 = new ScenarioAnalysis();
        s1.setId(1L);
        List<ScenarioAnalysis> list = Arrays.asList(s1);

        when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
        when(scenarioRepository.findByBusinessOrderByCreatedAtDesc(business)).thenReturn(list);

        List<ScenarioAnalysis> result = scenarioService.getScenarios(1L);

        assertEquals(1, result.size());
        assertEquals(s1, result.get(0));
    }
}
