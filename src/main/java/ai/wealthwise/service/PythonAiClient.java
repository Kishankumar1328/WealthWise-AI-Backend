package ai.wealthwise.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonAiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String AI_SERVICE_URL = "http://localhost:8000/api/v1/ai/forecast";

    @Data
    @Builder
    public static class ForecastRequest {
        private String businessId;
        private List<HistoryPoint> history;
        private List<Commitment> commitments;
        private int horizon;
    }

    @Data
    @Builder
    public static class HistoryPoint {
        private LocalDate date;
        private BigDecimal amount;
        private String type; // CREDIT/DEBIT
    }

    @Data
    @Builder
    public static class Commitment {
        private LocalDate dueDate;
        private BigDecimal amount;
        private String type; // AR/AP
    }

    @Data
    public static class ForecastResponse {
        private List<PredictionPoint> predictions;
        private Explainability explainability;
    }

    @Data
    public static class PredictionPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private BigDecimal expense;
        private BigDecimal confidence;
        private BigDecimal lowerBound;
        private BigDecimal upperBound;
    }

    @Data
    public static class Explainability {
        private String summary;
        private List<FeatureWeight> drivers;
    }

    @Data
    public static class FeatureWeight {
        private String feature;
        private Double weight;
    }

    public ForecastResponse getAdvancedForecast(ForecastRequest request) {
        try {
            log.info("Calling Python AI Service for business: {}", request.getBusinessId());
            return restTemplate.postForObject(AI_SERVICE_URL, request, ForecastResponse.class);
        } catch (Exception e) {
            log.error("Failed to connect to Python AI Service: {}. Falling back to internal engine.", e.getMessage());
            return null; // Fallback handled in service
        }
    }
}
