package ai.wealthwise.controller;

import ai.wealthwise.model.entity.CostOptimizationSuggestion;
import ai.wealthwise.service.CostOptimizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sme/cost-optimization")
@RequiredArgsConstructor
public class CostOptimizationController {

    private final CostOptimizationService optimizationService;

    @GetMapping("/{businessId}/suggestions")
    public ResponseEntity<List<CostOptimizationSuggestion>> getSuggestions(@PathVariable Long businessId) {
        return ResponseEntity.ok(optimizationService.getSuggestions(businessId));
    }

    @GetMapping("/{businessId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable Long businessId) {
        return ResponseEntity.ok(optimizationService.getSummary(businessId));
    }

    @PostMapping("/{businessId}/generate")
    public ResponseEntity<Map<String, Object>> generateSuggestions(@PathVariable Long businessId) {
        int count = optimizationService.generateOptimizationSuggestions(businessId);
        return ResponseEntity.ok(Map.of("newSuggestions", count, "status", "SUCCESS"));
    }
}
