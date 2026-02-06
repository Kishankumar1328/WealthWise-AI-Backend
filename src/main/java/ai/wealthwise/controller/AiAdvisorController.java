package ai.wealthwise.controller;

import ai.wealthwise.model.entity.User;
import ai.wealthwise.service.AiAdvisorClient;
import ai.wealthwise.service.DashboardService;
import ai.wealthwise.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Advisor Controller - Endpoints for AI-driven financial advice
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Advisor", description = "AI-powered financial recommendations")
@SecurityRequirement(name = "bearerAuth")
public class AiAdvisorController {

    private final AiAdvisorClient aiAdvisorClient;
    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    @PostMapping("/advice")
    @Operation(summary = "Get personalized financial advice")
    public ResponseEntity<Map<String, String>> getAdvice(@RequestBody Map<String, String> request) {
        User user = securityUtils.getCurrentUser();
        String query = request.getOrDefault("query", "Give me a summary of my financial health.");
        String language = request.getOrDefault("language", user.getPreferredLanguage());

        // Fetch real financial data for the user
        var summary = dashboardService.getSummary();

        String advice = aiAdvisorClient.getAdviceWithData(user.getId(), query, language, summary);
        return ResponseEntity.ok(Map.of("advice", advice));
    }
}
