package ai.wealthwise.controller;

import ai.wealthwise.model.entity.FinancialForecast;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.FinancialForecastingService;
import ai.wealthwise.service.SmeBusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sme/forecasting")
@RequiredArgsConstructor
public class ForecastingController {

    private final FinancialForecastingService forecastingService;
    private final SmeBusinessService smeBusinessService;
    private final UserRepository userRepository;

    @GetMapping("/{businessId}")
    public ResponseEntity<List<FinancialForecast>> getForecasts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        return ResponseEntity.ok(forecastingService.getForecasts(business));
    }

    @PostMapping("/{businessId}/refresh")
    public ResponseEntity<List<FinancialForecast>> refreshForecast(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @RequestParam(defaultValue = "90") int days) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        return ResponseEntity.ok(forecastingService.generateForecast(business, days));
    }

    @GetMapping("/{businessId}/summary")
    public ResponseEntity<Map<String, Object>> getForecastSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<FinancialForecast> forecasts = forecastingService.getForecasts(business);

        // Simple aggregation for summary
        double avgConfidence = forecasts.stream().mapToDouble(FinancialForecast::getConfidenceScore).average()
                .orElse(0.0);

        return ResponseEntity.ok(Map.of(
                "totalForecastPoints", forecasts.size(),
                "averageConfidence", avgConfidence,
                "horizonDays", forecasts.size() > 0 ? forecasts.size() : 0));
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
