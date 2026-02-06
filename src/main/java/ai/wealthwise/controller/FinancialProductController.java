package ai.wealthwise.controller;

import ai.wealthwise.model.entity.FinancialProduct;
import ai.wealthwise.model.entity.ProductRecommendation;
import ai.wealthwise.service.FinancialProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sme/financial-products")
@RequiredArgsConstructor
public class FinancialProductController {

    private final FinancialProductService productService;

    @GetMapping("/active")
    public ResponseEntity<List<FinancialProduct>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{businessId}/recommendations")
    public ResponseEntity<List<ProductRecommendation>> getRecommendations(@PathVariable Long businessId) {
        return ResponseEntity.ok(productService.getRecommendations(businessId));
    }

    @PostMapping("/{businessId}/generate")
    public ResponseEntity<Map<String, Object>> generateRecommendations(@PathVariable Long businessId) {
        int count = productService.generateRecommendations(businessId);
        return ResponseEntity.ok(Map.of("newRecommendations", count, "status", "SUCCESS"));
    }
}
