package ai.wealthwise.service;

import ai.wealthwise.model.dto.sme.CreditScoreResponse;
import ai.wealthwise.model.entity.*;
import ai.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Credit Scoring Service - Calculates SME creditworthiness using multiple
 * factors
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditScoringService {

    private final CreditScoreRepository creditScoreRepository;
    private final FinancialStatementRepository financialStatementRepository;
    private final InvoiceRepository invoiceRepository;
    private final LoanObligationRepository loanObligationRepository;
    private final TaxComplianceRepository taxComplianceRepository;
    private final AiAdvisorClient aiAdvisorClient;

    // Score weights
    private static final double PAYMENT_HISTORY_WEIGHT = 0.40;
    private static final double CREDIT_UTILIZATION_WEIGHT = 0.20;
    private static final double BUSINESS_AGE_WEIGHT = 0.15;
    private static final double FINANCIAL_HEALTH_WEIGHT = 0.15;
    private static final double COMPLIANCE_WEIGHT = 0.10;

    @Transactional
    public CreditScoreResponse calculateCreditScore(SmeBusiness business) {
        log.info("Calculating credit score for business: {}", business.getId());

        // Calculate component scores
        int paymentHistoryScore = calculatePaymentHistoryScore(business);
        int creditUtilizationScore = calculateCreditUtilizationScore(business);
        int businessAgeScore = calculateBusinessAgeScore(business);
        int financialHealthScore = calculateFinancialHealthScore(business);
        int complianceScore = calculateComplianceScore(business);

        // Calculate weighted overall score (0-900 scale)
        double weightedScore = (paymentHistoryScore * PAYMENT_HISTORY_WEIGHT) +
                (creditUtilizationScore * CREDIT_UTILIZATION_WEIGHT) +
                (businessAgeScore * BUSINESS_AGE_WEIGHT) +
                (financialHealthScore * FINANCIAL_HEALTH_WEIGHT) +
                (complianceScore * COMPLIANCE_WEIGHT);

        int overallScore = (int) Math.round((weightedScore / 100.0) * 900);
        overallScore = Math.max(0, Math.min(900, overallScore)); // Clamp to 0-900

        CreditScore.RiskLevel riskLevel = CreditScore.calculateRiskLevel(overallScore);

        // Calculate financial ratios
        FinancialRatios ratios = calculateFinancialRatios(business);

        // Generate heuristic risk factors and recommendations
        List<String> riskFactors = identifyRiskFactors(business, paymentHistoryScore, creditUtilizationScore,
                complianceScore);
        List<String> recommendations = generateRecommendations(riskFactors, riskLevel);

        // Enhance with AI insights if available
        try {
            var aiResponse = aiAdvisorClient.getAiCreditAnalysis(
                    business.getId(),
                    business.getBusinessName(),
                    business.getIndustryType().name(),
                    business.getAnnualTurnover() != null ? business.getAnnualTurnover().doubleValue() : 0.0,
                    overallScore,
                    ratios.currentRatio != null ? ratios.currentRatio.doubleValue() : 0.0,
                    ratios.debtEquityRatio != null ? ratios.debtEquityRatio.doubleValue() : 0.0,
                    ratios.profitMargin != null ? ratios.profitMargin.doubleValue() : 0.0,
                    invoiceRepository.sumOverdueReceivables(business, LocalDate.now()).doubleValue(),
                    loanObligationRepository.sumTotalOutstanding(business).doubleValue(),
                    complianceScore,
                    "en");

            if (aiResponse != null) {
                log.info("Successfully fetched AI credit analysis for business: {}", business.getId());
                // We could merge or override. Let's append AI summary to assessment if we had
                // one,
                // but here we primarily want the AI factors and recommendations if they are
                // better.
                if (aiResponse.getRiskFactors() != null && !aiResponse.getRiskFactors().isEmpty()
                        && !aiResponse.getRiskFactors().get(0).contains("See detailed")) {
                    riskFactors = aiResponse.getRiskFactors();
                }
                if (aiResponse.getRecommendations() != null && !aiResponse.getRecommendations().isEmpty()
                        && !aiResponse.getRecommendations().get(0).contains("See detailed")) {
                    recommendations = aiResponse.getRecommendations();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch AI credit analysis, falling back to heuristics: {}", e.getMessage());
        }

        // Calculate loan eligibility
        BigDecimal maxLoanEligible = calculateMaxLoanEligible(business, overallScore, ratios);
        BigDecimal suggestedInterestRate = calculateSuggestedInterestRate(overallScore);

        // Save credit score
        CreditScore creditScore = CreditScore.builder()
                .smeBusiness(business)
                .overallScore(overallScore)
                .paymentHistoryScore(paymentHistoryScore)
                .creditUtilizationScore(creditUtilizationScore)
                .businessAgeScore(businessAgeScore)
                .financialHealthScore(financialHealthScore)
                .complianceScore(complianceScore)
                .currentRatio(ratios.currentRatio)
                .debtEquityRatio(ratios.debtEquityRatio)
                .interestCoverageRatio(ratios.interestCoverageRatio)
                .profitMargin(ratios.profitMargin)
                .assetTurnoverRatio(ratios.assetTurnoverRatio)
                .riskLevel(riskLevel)
                .riskFactors(String.join("|", riskFactors))
                .recommendations(String.join("|", recommendations))
                .maxLoanEligible(maxLoanEligible)
                .suggestedInterestRate(suggestedInterestRate)
                .assessedAt(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();

        CreditScore saved = creditScoreRepository.save(creditScore);
        log.info("Credit score calculated: {} (Risk: {}) for business: {}", overallScore, riskLevel, business.getId());

        CreditScoreResponse response = CreditScoreResponse.fromEntity(saved);
        response.setRiskFactors(riskFactors);
        response.setRecommendations(recommendations);
        return response;
    }

    public Optional<CreditScoreResponse> getLatestScore(SmeBusiness business) {
        return creditScoreRepository.findLatestBySmeBusiness(business)
                .map(score -> {
                    CreditScoreResponse response = CreditScoreResponse.fromEntity(score);
                    if (score.getRiskFactors() != null) {
                        response.setRiskFactors(List.of(score.getRiskFactors().split("\\|")));
                    }
                    if (score.getRecommendations() != null) {
                        response.setRecommendations(List.of(score.getRecommendations().split("\\|")));
                    }
                    return response;
                });
    }

    private int calculatePaymentHistoryScore(SmeBusiness business) {
        Long overdueCount = invoiceRepository.countOverdueInvoices(business);
        BigDecimal overdueAmount = invoiceRepository.sumOverdueReceivables(business, LocalDate.now());
        BigDecimal totalOutstanding = invoiceRepository.sumOutstandingByType(business, Invoice.InvoiceType.RECEIVABLE);

        if (overdueCount == null || overdueCount == 0)
            return 100;
        if (totalOutstanding == null || totalOutstanding.compareTo(BigDecimal.ZERO) == 0)
            return 80;

        double overdueRatio = overdueAmount != null
                ? overdueAmount.divide(totalOutstanding, 4, RoundingMode.HALF_UP).doubleValue()
                : 0;

        if (overdueRatio > 0.5)
            return 30;
        if (overdueRatio > 0.3)
            return 50;
        if (overdueRatio > 0.1)
            return 70;
        return 85;
    }

    private int calculateCreditUtilizationScore(SmeBusiness business) {
        BigDecimal totalDebt = loanObligationRepository.sumTotalOutstanding(business);
        BigDecimal annualTurnover = business.getAnnualTurnover();

        if (totalDebt == null || annualTurnover == null || annualTurnover.compareTo(BigDecimal.ZERO) == 0) {
            return 80; // No debt = good
        }

        double utilizationRatio = totalDebt.divide(annualTurnover, 4, RoundingMode.HALF_UP).doubleValue();

        if (utilizationRatio > 1.0)
            return 20;
        if (utilizationRatio > 0.7)
            return 40;
        if (utilizationRatio > 0.5)
            return 60;
        if (utilizationRatio > 0.3)
            return 80;
        return 95;
    }

    private int calculateBusinessAgeScore(SmeBusiness business) {
        if (business.getRegistrationDate() == null)
            return 50;

        long years = ChronoUnit.YEARS.between(business.getRegistrationDate(), LocalDate.now());
        if (years >= 10)
            return 100;
        if (years >= 5)
            return 85;
        if (years >= 3)
            return 70;
        if (years >= 1)
            return 50;
        return 30;
    }

    private int calculateFinancialHealthScore(SmeBusiness business) {
        FinancialRatios ratios = calculateFinancialRatios(business);
        int score = 50; // Base score

        // Current ratio (ideal: 1.5-3.0)
        if (ratios.currentRatio != null) {
            double cr = ratios.currentRatio.doubleValue();
            if (cr >= 1.5 && cr <= 3.0)
                score += 15;
            else if (cr >= 1.0)
                score += 8;
            else
                score -= 10;
        }

        // Debt-to-equity (ideal: < 1.5)
        if (ratios.debtEquityRatio != null) {
            double de = ratios.debtEquityRatio.doubleValue();
            if (de < 0.5)
                score += 15;
            else if (de < 1.0)
                score += 10;
            else if (de < 1.5)
                score += 5;
            else
                score -= 10;
        }

        // Profit margin (ideal: > 10%)
        if (ratios.profitMargin != null) {
            double pm = ratios.profitMargin.doubleValue();
            if (pm > 20)
                score += 20;
            else if (pm > 10)
                score += 15;
            else if (pm > 5)
                score += 10;
            else
                score -= 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    private int calculateComplianceScore(SmeBusiness business) {
        Double avgScore = taxComplianceRepository.calculateAverageComplianceScore(business);
        Long lateFilings = taxComplianceRepository.countLateFilings(business);

        if (avgScore != null) {
            int base = avgScore.intValue();
            if (lateFilings != null && lateFilings > 0) {
                base -= (int) (lateFilings * 5);
            }
            return Math.max(0, Math.min(100, base));
        }
        return 70; // Default score if no compliance data
    }

    private FinancialRatios calculateFinancialRatios(SmeBusiness business) {
        List<FinancialStatement> statements = financialStatementRepository.findLatestByType(
                business, FinancialStatement.StatementType.BALANCE_SHEET);

        FinancialRatios ratios = new FinancialRatios();

        if (!statements.isEmpty()) {
            FinancialStatement bs = statements.get(0);

            if (bs.getCurrentAssets() != null && bs.getCurrentLiabilities() != null &&
                    bs.getCurrentLiabilities().compareTo(BigDecimal.ZERO) > 0) {
                ratios.currentRatio = bs.getCurrentAssets()
                        .divide(bs.getCurrentLiabilities(), 2, RoundingMode.HALF_UP);
            }

            if (bs.getTotalLiabilities() != null && bs.getEquity() != null &&
                    bs.getEquity().compareTo(BigDecimal.ZERO) > 0) {
                ratios.debtEquityRatio = bs.getTotalLiabilities()
                        .divide(bs.getEquity(), 2, RoundingMode.HALF_UP);
            }

            if (bs.getNetProfit() != null && bs.getTotalRevenue() != null &&
                    bs.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
                ratios.profitMargin = bs.getNetProfit()
                        .divide(bs.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        return ratios;
    }

    private List<String> identifyRiskFactors(SmeBusiness business, int paymentScore, int utilizationScore,
            int complianceScore) {
        List<String> risks = new ArrayList<>();

        if (paymentScore < 50)
            risks.add("High overdue receivables ratio");
        if (utilizationScore < 50)
            risks.add("Excessive credit utilization");
        if (complianceScore < 60)
            risks.add("Tax compliance issues detected");

        Long overdueFilings = taxComplianceRepository.countLateFilings(business);
        if (overdueFilings != null && overdueFilings > 2) {
            risks.add("Multiple late GST filings");
        }

        List<LoanObligation> overdueLoans = loanObligationRepository.findOverdueLoans(business);
        if (!overdueLoans.isEmpty()) {
            risks.add("Active loan defaults/overdue EMIs");
        }

        if (risks.isEmpty()) {
            risks.add("No significant risk factors identified");
        }

        return risks;
    }

    private List<String> generateRecommendations(List<String> riskFactors, CreditScore.RiskLevel riskLevel) {
        List<String> recommendations = new ArrayList<>();

        if (riskFactors.contains("High overdue receivables ratio")) {
            recommendations.add("Implement stricter credit control and follow-up processes");
        }
        if (riskFactors.contains("Excessive credit utilization")) {
            recommendations.add("Consider debt restructuring or additional equity infusion");
        }
        if (riskFactors.contains("Tax compliance issues detected")) {
            recommendations.add("Ensure all GST filings are completed on time");
        }

        if (riskLevel == CreditScore.RiskLevel.EXCELLENT) {
            recommendations.add("Explore competitive credit options to leverage your strong profile");
        } else if (riskLevel == CreditScore.RiskLevel.CRITICAL) {
            recommendations.add("Urgently seek financial restructuring advice");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Maintain current financial practices");
        }

        return recommendations;
    }

    private BigDecimal calculateMaxLoanEligible(SmeBusiness business, int score, FinancialRatios ratios) {
        BigDecimal turnover = business.getAnnualTurnover();
        if (turnover == null)
            return BigDecimal.ZERO;

        double multiplier;
        if (score >= 750)
            multiplier = 0.5;
        else if (score >= 650)
            multiplier = 0.35;
        else if (score >= 550)
            multiplier = 0.2;
        else
            multiplier = 0.1;

        return turnover.multiply(BigDecimal.valueOf(multiplier)).setScale(0, RoundingMode.DOWN);
    }

    private BigDecimal calculateSuggestedInterestRate(int score) {
        if (score >= 750)
            return BigDecimal.valueOf(9.5);
        if (score >= 650)
            return BigDecimal.valueOf(12.0);
        if (score >= 550)
            return BigDecimal.valueOf(15.0);
        if (score >= 400)
            return BigDecimal.valueOf(18.0);
        return BigDecimal.valueOf(24.0);
    }

    private static class FinancialRatios {
        BigDecimal currentRatio;
        BigDecimal debtEquityRatio;
        BigDecimal interestCoverageRatio;
        BigDecimal profitMargin;
        BigDecimal assetTurnoverRatio;
    }
}
