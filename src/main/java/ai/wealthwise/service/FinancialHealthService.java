package ai.wealthwise.service;

import ai.wealthwise.model.dto.sme.FinancialHealthResponse;
import ai.wealthwise.model.entity.*;
import ai.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Financial Health Service - Aggregates all financial data for SME dashboard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialHealthService {

    private final CreditScoreRepository creditScoreRepository;
    private final InvoiceRepository invoiceRepository;
    private final CashFlowRecordRepository cashFlowRecordRepository;
    private final LoanObligationRepository loanObligationRepository;
    private final TaxComplianceRepository taxComplianceRepository;
    private final FinancialStatementRepository financialStatementRepository;

    public FinancialHealthResponse getFinancialHealthSummary(SmeBusiness business) {
        log.info("Generating financial health summary for business: {}", business.getId());

        // Credit Score
        Optional<CreditScore> latestScore = creditScoreRepository.findLatestBySmeBusiness(business);
        Integer creditScore = latestScore.map(CreditScore::getOverallScore).orElse(null);
        String riskLevel = latestScore.map(s -> s.getRiskLevel().name()).orElse("NOT_ASSESSED");

        // Cash Flow (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        BigDecimal totalInflow = cashFlowRecordRepository.sumInflowsForPeriod(business, startDate, endDate);
        BigDecimal totalOutflow = cashFlowRecordRepository.sumOutflowsForPeriod(business, startDate, endDate);
        totalInflow = totalInflow != null ? totalInflow : BigDecimal.ZERO;
        totalOutflow = totalOutflow != null ? totalOutflow : BigDecimal.ZERO;
        BigDecimal netCashFlow = totalInflow.subtract(totalOutflow);

        // Receivables & Payables
        BigDecimal totalReceivables = invoiceRepository.sumOutstandingByType(business, Invoice.InvoiceType.RECEIVABLE);
        BigDecimal totalPayables = invoiceRepository.sumOutstandingByType(business, Invoice.InvoiceType.PAYABLE);
        BigDecimal overdueReceivables = invoiceRepository.sumOverdueReceivables(business, LocalDate.now());
        Long overdueCount = invoiceRepository.countOverdueInvoices(business);
        totalReceivables = totalReceivables != null ? totalReceivables : BigDecimal.ZERO;
        totalPayables = totalPayables != null ? totalPayables : BigDecimal.ZERO;
        overdueReceivables = overdueReceivables != null ? overdueReceivables : BigDecimal.ZERO;

        // Loans
        BigDecimal totalDebt = loanObligationRepository.sumTotalOutstanding(business);
        BigDecimal monthlyEmi = loanObligationRepository.sumMonthlyEmiObligation(business);
        Long activeLoans = loanObligationRepository.countActiveLoans(business);
        totalDebt = totalDebt != null ? totalDebt : BigDecimal.ZERO;
        monthlyEmi = monthlyEmi != null ? monthlyEmi : BigDecimal.ZERO;

        // Tax Compliance
        Double avgComplianceScore = taxComplianceRepository.calculateAverageComplianceScore(business);
        List<TaxCompliance> pendingFilings = taxComplianceRepository.findPendingFilings(business);
        List<TaxCompliance> overdueFilings = taxComplianceRepository.findOverdueFilings(business, LocalDate.now());

        // Financial Ratios from latest statement
        BigDecimal currentRatio = null;
        BigDecimal debtEquityRatio = null;
        BigDecimal profitMargin = null;
        List<FinancialStatement> statements = financialStatementRepository.findLatestByType(
                business, FinancialStatement.StatementType.BALANCE_SHEET);
        if (!statements.isEmpty()) {
            FinancialStatement bs = statements.get(0);
            if (bs.getCurrentAssets() != null && bs.getCurrentLiabilities() != null &&
                    bs.getCurrentLiabilities().compareTo(BigDecimal.ZERO) > 0) {
                currentRatio = bs.getCurrentAssets().divide(bs.getCurrentLiabilities(), 2, RoundingMode.HALF_UP);
            }
            if (bs.getTotalLiabilities() != null && bs.getEquity() != null &&
                    bs.getEquity().compareTo(BigDecimal.ZERO) > 0) {
                debtEquityRatio = bs.getTotalLiabilities().divide(bs.getEquity(), 2, RoundingMode.HALF_UP);
            }
            if (bs.getNetProfit() != null && bs.getTotalRevenue() != null &&
                    bs.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
                profitMargin = bs.getNetProfit().divide(bs.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        // Days of cash runway
        Integer daysOfCashRunway = null;
        if (totalOutflow.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyBurn = totalOutflow.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            if (dailyBurn.compareTo(BigDecimal.ZERO) > 0) {
                // Assume current cash is net inflow
                daysOfCashRunway = netCashFlow.divide(dailyBurn, 0, RoundingMode.DOWN).intValue();
                daysOfCashRunway = Math.max(0, daysOfCashRunway);
            }
        }

        // Generate alerts
        List<FinancialHealthResponse.HealthAlert> alerts = generateAlerts(
                creditScore, overdueReceivables, pendingFilings.size(), overdueFilings.size(), totalDebt, business);

        // Build response
        return FinancialHealthResponse.builder()
                .smeBusinessId(business.getId())
                .businessName(business.getBusinessName())
                .industryType(business.getIndustryType().name())
                .creditScore(creditScore)
                .riskLevel(riskLevel)
                .totalCashInflow(totalInflow)
                .totalCashOutflow(totalOutflow)
                .netCashFlow(netCashFlow)
                .daysOfCashRunway(daysOfCashRunway)
                .totalReceivables(totalReceivables)
                .totalPayables(totalPayables)
                .overdueReceivables(overdueReceivables)
                .overdueInvoiceCount(overdueCount != null ? overdueCount : 0L)
                .totalDebt(totalDebt)
                .monthlyEmiObligation(monthlyEmi)
                .activeLoansCount(activeLoans != null ? activeLoans : 0L)
                .complianceScore(avgComplianceScore != null ? avgComplianceScore.intValue() : null)
                .pendingFilings((long) pendingFilings.size())
                .overdueFilings((long) overdueFilings.size())
                .currentRatio(currentRatio)
                .debtEquityRatio(debtEquityRatio)
                .profitMargin(profitMargin)
                .alerts(alerts)
                .build();
    }

    private List<FinancialHealthResponse.HealthAlert> generateAlerts(
            Integer creditScore, BigDecimal overdueReceivables, int pendingFilings,
            int overdueFilings, BigDecimal totalDebt, SmeBusiness business) {

        List<FinancialHealthResponse.HealthAlert> alerts = new ArrayList<>();

        // Credit score alerts
        if (creditScore != null && creditScore < 550) {
            alerts.add(FinancialHealthResponse.HealthAlert.builder()
                    .type("CRITICAL")
                    .category("CREDIT")
                    .message("Your credit score is below acceptable levels")
                    .actionRequired("Review credit improvement recommendations")
                    .build());
        }

        // Overdue receivables
        if (overdueReceivables.compareTo(BigDecimal.valueOf(100000)) > 0) {
            alerts.add(FinancialHealthResponse.HealthAlert.builder()
                    .type("WARNING")
                    .category("CASH_FLOW")
                    .message("High overdue receivables detected: ₹" + overdueReceivables.setScale(0, RoundingMode.DOWN))
                    .actionRequired("Follow up on overdue payments")
                    .build());
        }

        // Tax compliance
        if (overdueFilings > 0) {
            alerts.add(FinancialHealthResponse.HealthAlert.builder()
                    .type("CRITICAL")
                    .category("COMPLIANCE")
                    .message(overdueFilings + " tax filing(s) are overdue")
                    .actionRequired("File immediately to avoid penalties")
                    .build());
        } else if (pendingFilings > 0) {
            alerts.add(FinancialHealthResponse.HealthAlert.builder()
                    .type("INFO")
                    .category("COMPLIANCE")
                    .message(pendingFilings + " upcoming tax filing(s) due")
                    .actionRequired("Prepare documents for timely filing")
                    .build());
        }

        // Debt level
        if (business.getAnnualTurnover() != null && totalDebt.compareTo(business.getAnnualTurnover()) > 0) {
            alerts.add(FinancialHealthResponse.HealthAlert.builder()
                    .type("WARNING")
                    .category("DEBT")
                    .message("Total debt exceeds annual turnover")
                    .actionRequired("Consider debt restructuring")
                    .build());
        }

        return alerts;
    }
}
