package ai.wealthwise.service;

import ai.wealthwise.model.entity.TaxCompliance;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.EWayBill;
import ai.wealthwise.repository.TaxComplianceRepository;
import ai.wealthwise.repository.EWayBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxComplianceService {

    private final TaxComplianceRepository taxComplianceRepository;
    private final EWayBillRepository eWayBillRepository;

    @Transactional
    public TaxCompliance createFiling(SmeBusiness business, TaxCompliance.FilingType filingType,
            String filingPeriod, LocalDate dueDate,
            BigDecimal taxLiability, BigDecimal inputTaxCredit) {

        BigDecimal netTaxPayable = taxLiability.subtract(inputTaxCredit);

        TaxCompliance filing = TaxCompliance.builder()
                .smeBusiness(business)
                .filingType(filingType)
                .filingPeriod(filingPeriod)
                .dueDate(dueDate)
                .filingStatus(TaxCompliance.FilingStatus.PENDING)
                .taxLiability(taxLiability)
                .inputTaxCredit(inputTaxCredit)
                .netTaxPayable(netTaxPayable)
                .build();

        return taxComplianceRepository.save(filing);
    }

    @Transactional
    public TaxCompliance markAsFiled(Long filingId, LocalDate filedDate, String arnNumber,
            BigDecimal taxPaid, String filingReference) {
        TaxCompliance filing = taxComplianceRepository.findById(filingId)
                .orElseThrow(() -> new IllegalArgumentException("Filing not found"));

        filing.setFiledDate(filedDate);
        filing.setArnNumber(arnNumber);
        filing.setTaxPaid(taxPaid);
        filing.setFilingReference(filingReference);

        if (filedDate.isAfter(filing.getDueDate())) {
            filing.setFilingStatus(TaxCompliance.FilingStatus.FILED_LATE);
            int daysDelayed = (int) ChronoUnit.DAYS.between(filing.getDueDate(), filedDate);
            filing.setDaysDelayed(daysDelayed);
            // Calculate compliance score (100 - days delayed, min 0)
            filing.setComplianceScore(Math.max(0, 100 - daysDelayed));
        } else {
            filing.setFilingStatus(TaxCompliance.FilingStatus.FILED_ON_TIME);
            filing.setDaysDelayed(0);
            filing.setComplianceScore(100);
        }

        return taxComplianceRepository.save(filing);
    }

    public List<TaxCompliance> getAllFilings(SmeBusiness business) {
        return taxComplianceRepository.findBySmeBusinessOrderByDueDateDesc(business);
    }

    public List<TaxCompliance> getPendingFilings(SmeBusiness business) {
        return taxComplianceRepository.findPendingFilings(business);
    }

    public List<TaxCompliance> getOverdueFilings(SmeBusiness business) {
        return taxComplianceRepository.findOverdueFilings(business, LocalDate.now());
    }

    public List<TaxCompliance> getUpcomingFilings(SmeBusiness business, int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return taxComplianceRepository.findUpcomingFilings(business, startDate, endDate);
    }

    public Double getAverageComplianceScore(SmeBusiness business) {
        return taxComplianceRepository.calculateAverageComplianceScore(business);
    }

    @Transactional
    public void updateOverdueStatus(SmeBusiness business) {
        LocalDate today = LocalDate.now();
        List<TaxCompliance> pendingFilings = taxComplianceRepository.findPendingFilings(business);

        for (TaxCompliance filing : pendingFilings) {
            if (filing.getDueDate().isBefore(today) && filing.getFilingStatus() == TaxCompliance.FilingStatus.PENDING) {
                filing.setFilingStatus(TaxCompliance.FilingStatus.OVERDUE);
                filing.setDaysDelayed((int) ChronoUnit.DAYS.between(filing.getDueDate(), today));
                taxComplianceRepository.save(filing);
            }
        }
    }

    /**
     * Generate standard GST filing schedule for financial year
     */
    @Transactional
    public void generateGstFilingSchedule(SmeBusiness business, String fiscalYear) {
        // GSTR-3B is due on 20th of next month
        // GSTR-1 is due on 11th of next month

        String[] months = { "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar" };
        int[] years = fiscalYear.equals("2025-2026")
                ? new int[] { 2025, 2025, 2025, 2025, 2025, 2025, 2025, 2025, 2025, 2026, 2026, 2026 }
                : new int[] { 2026, 2026, 2026, 2026, 2026, 2026, 2026, 2026, 2026, 2027, 2027, 2027 };

        for (int i = 0; i < 12; i++) {
            int month = (i + 4) % 12 + 1; // April=4, March=3
            int year = years[i];
            int nextMonth = (month % 12) + 1;
            int nextYear = month == 12 ? year + 1 : year;

            // GSTR-1 due on 11th
            createFiling(business, TaxCompliance.FilingType.GSTR1,
                    months[i] + "-" + year,
                    LocalDate.of(nextYear, nextMonth, 11),
                    BigDecimal.ZERO, BigDecimal.ZERO);

            // GSTR-3B due on 20th
            createFiling(business, TaxCompliance.FilingType.GSTR3B,
                    months[i] + "-" + year,
                    LocalDate.of(nextYear, nextMonth, 20),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        log.info("Generated GST filing schedule for business {} for FY {}", business.getId(), fiscalYear);
    }

    // ================= NEW GST METHODS =================

    /**
     * Mock syncing logic from GSTN Portal
     */
    @Transactional
    public void syncGstData(SmeBusiness business) {
        // Mocking: Update random pending filings to FILED_ON_TIME to simulate fetching
        // status
        List<TaxCompliance> pending = getPendingFilings(business);
        Random rand = new Random();

        for (TaxCompliance filing : pending) {
            if (rand.nextBoolean()) { // 50% chance to find a filed return
                filing.setFilingStatus(TaxCompliance.FilingStatus.FILED_ON_TIME);
                filing.setFiledDate(filing.getDueDate().minusDays(rand.nextInt(5)));
                filing.setArnNumber("ARN" + System.currentTimeMillis() + rand.nextInt(100));
                filing.setTaxPaid(filing.getNetTaxPayable());
                filing.setComplianceScore(100);
                filing.setNotes("Auto-synced from GSTN");
                taxComplianceRepository.save(filing);
            }
        }

        // Mock Import specific GSTR-1 Data if not exists
        if (taxComplianceRepository.count() < 5) {
            generateGstFilingSchedule(business, "2025-2026");
        }

        // Generate Mock E-Way Bills if empty
        if (eWayBillRepository.findBySmeBusiness(business).isEmpty()) {
            generateMockEWayBills(business);
        }

        log.info("Synced GST data from GSTN for business {}", business.getId());
    }

    private void generateMockEWayBills(SmeBusiness business) {
        for (int i = 0; i < 5; i++) {
            EWayBill bill = EWayBill.builder()
                    .smeBusiness(business)
                    .ewayBillNumber("EWB" + (1000000000L + new Random().nextInt(900000000)))
                    .generatedDate(LocalDateTime.now().minusDays(new Random().nextInt(30)))
                    .validUpto(LocalDateTime.now().plusDays(new Random().nextInt(5)))
                    .status("ACTIVE")
                    .consignorGstin(business.getGstin() != null ? business.getGstin() : "27ABCDE1234F1Z5")
                    .consigneeGstin("29XYZABC9876Q1Z2")
                    .itemValue(BigDecimal.valueOf(50000 + new Random().nextInt(100000)))
                    .taxValue(BigDecimal.valueOf(9000 + new Random().nextInt(18000)))
                    .totalValue(BigDecimal.ZERO) // Set later
                    .createdAt(LocalDateTime.now())
                    .build();

            bill.setTotalValue(bill.getItemValue().add(bill.getTaxValue()));
            eWayBillRepository.save(bill);
        }
    }

    public List<EWayBill> getEWayBills(SmeBusiness business) {
        return eWayBillRepository.findBySmeBusiness(business);
    }

    public String validateReturn(Long filingId) {
        TaxCompliance filing = taxComplianceRepository.findById(filingId)
                .orElseThrow(() -> new IllegalArgumentException("Filing not found"));

        // Mock Validation Logic
        if (filing.getTaxLiability().compareTo(filing.getInputTaxCredit()) < 0) {
            return "WARNING: ITC exceeds Liability significantly. Risk of Audit.";
        }
        if (filing.getFilingStatus() == TaxCompliance.FilingStatus.OVERDUE) {
            return "CRITICAL: Return is Overdue. Penalty applicable.";
        }
        return "VALID: Return data seems consistent.";
    }
}
