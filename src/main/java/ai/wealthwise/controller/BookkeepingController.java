package ai.wealthwise.controller;

import ai.wealthwise.model.entity.BookkeepingRule;
import ai.wealthwise.service.BookkeepingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sme/bookkeeping")
@RequiredArgsConstructor
public class BookkeepingController {

    private final BookkeepingService bookkeepingService;

    @PostMapping("/{businessId}/rules")
    public ResponseEntity<BookkeepingRule> createRule(@PathVariable Long businessId,
            @RequestBody BookkeepingRule rule) {
        return ResponseEntity.ok(bookkeepingService.createRule(businessId, rule));
    }

    @GetMapping("/{businessId}/rules")
    public ResponseEntity<List<BookkeepingRule>> getRules(@PathVariable Long businessId) {
        return ResponseEntity.ok(bookkeepingService.getRules(businessId));
    }

    @PostMapping("/{businessId}/run-categorization")
    public ResponseEntity<Map<String, Object>> runAutoCategorization(@PathVariable Long businessId) {
        int count = bookkeepingService.runAutoCategorization(businessId);
        return ResponseEntity.ok(Map.of("processed", count, "status", "SUCCESS"));
    }

    @PostMapping("/{businessId}/scan-duplicates")
    public ResponseEntity<Map<String, Object>> scanDuplicates(@PathVariable Long businessId) {
        int count = bookkeepingService.scanForDuplicates(businessId);
        return ResponseEntity.ok(Map.of("duplicatesFound", count, "status", "SUCCESS"));
    }

    @DeleteMapping("/{businessId}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long businessId, @PathVariable Long ruleId) {
        bookkeepingService.deleteRule(businessId, ruleId);
        return ResponseEntity.noContent().build();
    }
}
