package ai.wealthwise.service;

import ai.wealthwise.model.entity.*;
import ai.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FinancialProductService {

    private final FinancialProductRepository productRepository;
    private final ProductRecommendationRepository recommendationRepository;
    private final SmeBusinessRepository businessRepository;
    private final CreditScoreRepository creditScoreRepository;

    /**
     * Module 8: Match financial products to businesses based on eligibility logic.
     */
    @Transactional
    public int generateRecommendations(Long businessId) {
        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        // Get latest credit score
        Optional<CreditScore> scoreOpt = creditScoreRepository.findLatestBySmeBusiness(business);
        int creditScore = scoreOpt.map(CreditScore::getOverallScore).orElse(600); // Default safe score

        BigDecimal annualRevenue = business.getAnnualRevenue(); // Assuming this field exists and is updated

        List<FinancialProduct> activeProducts = productRepository.findByIsActiveTrue();
        if (activeProducts.isEmpty()) {
            seedDemoProducts();
            activeProducts = productRepository.findByIsActiveTrue();
        }
        int newRecs = 0;

        for (FinancialProduct product : activeProducts) {
            if (isEligible(product, creditScore, annualRevenue)) {
                createRecommendation(business, product);
                newRecs++;
            }
        }
        return newRecs;
    }

    private boolean isEligible(FinancialProduct product, int creditScore, BigDecimal annualRevenue) {
        // Credit Score Check
        if (product.getMinCreditScoreRequired() != null && creditScore < product.getMinCreditScoreRequired()) {
            return false;
        }

        // Revenue Check
        if (product.getMinAnnualRevenue() != null
                && (annualRevenue == null || annualRevenue.compareTo(product.getMinAnnualRevenue()) < 0)) {
            return false;
        }

        return true;
    }

    private void createRecommendation(SmeBusiness business, FinancialProduct product) {
        // Check duplicate
        List<ProductRecommendation> existing = recommendationRepository.findByBusinessAndStatus(business,
                ProductRecommendation.Status.NEW);
        if (existing.stream().anyMatch(r -> r.getProduct().getId().equals(product.getId()))) {
            return;
        }

        double matchScore = calculateMatchScore(business, product);

        ProductRecommendation recommendation = ProductRecommendation.builder()
                .business(business)
                .product(product)
                .matchScore(matchScore)
                .matchReason("Meets credit score and revenue criteria. Competitive interest rate.")
                .estimatedEligibilityAmount(
                        product.getMaxLoanAmount() != null ? product.getMaxLoanAmount().min(BigDecimal.valueOf(5000000))
                                : BigDecimal.valueOf(1000000)) // Cap logic
                .status(ProductRecommendation.Status.NEW)
                .build();

        recommendationRepository.save(recommendation);
    }

    private double calculateMatchScore(SmeBusiness business, FinancialProduct product) {
        // Simple logic: higher revenue = better match
        return 85.0; // Placeholder
    }

    public List<ProductRecommendation> getRecommendations(Long businessId) {
        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        return recommendationRepository.findByBusinessOrderByMatchScoreDesc(business);
    }

    public List<FinancialProduct> getAllProducts() {
        List<FinancialProduct> products = productRepository.findByIsActiveTrue();
        if (products.isEmpty()) {
            seedDemoProducts();
            return productRepository.findByIsActiveTrue();
        }
        return products;
    }

    private void seedDemoProducts() {
        System.out.println("Seeding Demo Financial Products...");

        createProduct("HDFC Bank", "Growth Capital Term Loan", FinancialProduct.ProductType.TERM_LOAN,
                "Unsecured business loan for expansion and working capital needs.",
                10.5, 14.0, 1.5, 650, 5000000.0, 20000000.0);

        createProduct("ICICI Bank", "Smart Business Overdraft", FinancialProduct.ProductType.WORKING_CAPITAL_LOAN,
                "Flexible overdraft facility linked to business turnover.",
                9.0, 12.0, 0.5, 700, 10000000.0, 50000000.0);

        createProduct("KredX", "Invoice Discounting", FinancialProduct.ProductType.INVOICE_DISCOUNTING,
                "Get instant cash against unpaid invoices within 24-72 hours.",
                12.0, 18.0, 2.0, 600, 2000000.0, 10000000.0);

        createProduct("Bajaj Finserv", "Machinery Loan", FinancialProduct.ProductType.EQUIPMENT_FINANCE,
                "Finance for purchasing new or refurbished machinery.",
                11.0, 13.5, 1.0, 650, 3000000.0, 15000000.0);

        createProduct("Amex", "Platinum Corporate Card", FinancialProduct.ProductType.BUSINESS_CREDIT_CARD,
                "Premium travel and lifestyle benefits for business owners.",
                36.0, 42.0, 0.0, 750, 10000000.0, 5000000.0);

        System.out.println("Demo Products Seeded.");
    }

    private void createProduct(String provider, String name, FinancialProduct.ProductType type, String desc,
            double minRate, double maxRate, double fee, int minScore, double minRev, double maxAmt) {
        FinancialProduct product = FinancialProduct.builder()
                .providerName(provider)
                .productName(name)
                .productType(type)
                .description(desc)
                .interestRateMin(BigDecimal.valueOf(minRate))
                .interestRateMax(BigDecimal.valueOf(maxRate))
                .processingFeePercentage(BigDecimal.valueOf(fee))
                .minCreditScoreRequired(minScore)
                .minAnnualRevenue(BigDecimal.valueOf(minRev))
                .maxLoanAmount(BigDecimal.valueOf(maxAmt))
                .isActive(true)
                .featuresJson("[\"Quick Approval\", " + "\"Digital Process\"]")
                .build();
        productRepository.save(product);
    }
}
