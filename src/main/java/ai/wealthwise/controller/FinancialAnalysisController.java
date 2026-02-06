package ai.wealthwise.controller;

import ai.wealthwise.model.dto.sme.CreditScoreResponse;
import ai.wealthwise.model.dto.sme.FinancialHealthResponse;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.CreditScoringService;
import ai.wealthwise.service.FinancialHealthService;
import ai.wealthwise.service.SmeBusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sme/analysis")
@RequiredArgsConstructor
public class FinancialAnalysisController {

    private final FinancialHealthService financialHealthService;
    private final CreditScoringService creditScoringService;
    private final SmeBusinessService smeBusinessService;
    private final UserRepository userRepository;

    @GetMapping("/{businessId}/health")
    public ResponseEntity<FinancialHealthResponse> getFinancialHealth(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        FinancialHealthResponse response = financialHealthService.getFinancialHealthSummary(business);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{businessId}/credit-score")
    public ResponseEntity<CreditScoreResponse> getCreditScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);

        return creditScoringService.getLatestScore(business)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // Calculate new score if none exists
                    CreditScoreResponse newScore = creditScoringService.calculateCreditScore(business);
                    return ResponseEntity.ok(newScore);
                });
    }

    @PostMapping("/{businessId}/credit-score/refresh")
    public ResponseEntity<CreditScoreResponse> refreshCreditScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        CreditScoreResponse response = creditScoringService.calculateCreditScore(business);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{businessId}/ratios")
    public ResponseEntity<Map<String, Object>> getFinancialRatios(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        FinancialHealthResponse health = financialHealthService.getFinancialHealthSummary(business);

        Map<String, Object> ratios = new HashMap<>();
        ratios.put("currentRatio", health.getCurrentRatio());
        ratios.put("debtEquityRatio", health.getDebtEquityRatio());
        ratios.put("profitMargin", health.getProfitMargin());
        ratios.put("creditScore", health.getCreditScore());
        ratios.put("complianceScore", health.getComplianceScore());

        return ResponseEntity.ok(ratios);
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
