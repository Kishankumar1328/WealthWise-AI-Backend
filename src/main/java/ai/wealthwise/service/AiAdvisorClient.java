package ai.wealthwise.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AI Advisor Client - Integrates with Python FastAPI AI Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAdvisorClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    /**
     * Get financial advice from AI service with context data
     */
    public String getAdviceWithData(Long userId, String query, String language,
            ai.wealthwise.model.dto.dashboard.DashboardSummaryResponse summary) {
        String url = aiServiceUrl + "/api/v1/ai/advice";

        AdviceRequest request = new AdviceRequest();
        request.setUserId(userId);
        request.setQuery(query);
        request.setLanguage(language);
        request.setFinancialSummary(summary);

        try {
            AdviceResponse response = restTemplate.postForObject(url, request, AdviceResponse.class);
            return response != null ? response.getAdvice() : "Sorry, I couldn't generate advice at this time.";
        } catch (Exception e) {
            log.error("Error calling AI service for advice", e);
            return "Unable to connect to financial advisor AI. Please try again later.";
        }
    }

    /**
     * Get professional credit analysis from AI service
     */
    public ai.wealthwise.model.dto.sme.CreditScoreResponse getAiCreditAnalysis(
            Long businessId, String businessName, String industry, Double turnover,
            Integer score, Double currentRatio, Double debtEquity, Double profitMargin,
            Double overdue, Double debt, Integer compliance, String language) {

        String url = aiServiceUrl + "/api/v1/ai/credit-analysis";

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("business_name", businessName);
        request.put("industry_type", industry);
        request.put("annual_turnover", turnover);
        request.put("credit_score", score);
        request.put("current_ratio", currentRatio != null ? currentRatio : 0.0);
        request.put("debt_equity_ratio", debtEquity != null ? debtEquity : 0.0);
        request.put("profit_margin", profitMargin != null ? profitMargin : 0.0);
        request.put("overdue_receivables", overdue);
        request.put("total_debt", debt);
        request.put("gst_compliance_score", compliance);
        request.put("language", language);

        try {
            return restTemplate.postForObject(url, request, ai.wealthwise.model.dto.sme.CreditScoreResponse.class);
        } catch (Exception e) {
            log.error("Error calling AI service for credit analysis", e);
            return null;
        }
    }

    /**
     * Get risk assessment from AI service
     */
    public Map<String, Object> getAiRiskAssessment(
            String businessName, String industry, String trend, Double overdue,
            Integer runway, Integer pendingGst, Integer defaults, String language) {

        String url = aiServiceUrl + "/api/v1/ai/risk-assessment";

        Map<String, Object> request = Map.of(
                "business_name", businessName,
                "industry_type", industry,
                "cash_flow_trend", trend,
                "overdue_amount", overdue,
                "days_cash_runway", runway,
                "pending_gst_filings", pendingGst,
                "loan_defaults", defaults,
                "language", language);

        try {
            return restTemplate.postForObject(url, request, Map.class);
        } catch (Exception e) {
            log.error("Error calling AI service for risk assessment", e);
            return Map.of("error", "AI service unavailable");
        }
    }

    @Data
    public static class AdviceRequest {
        private Long userId;
        private String query;
        private String language;
        private ai.wealthwise.model.dto.dashboard.DashboardSummaryResponse financialSummary;
    }

    @Data
    public static class AdviceResponse {
        private String advice;
        private Map<String, Object> data;
    }
}
