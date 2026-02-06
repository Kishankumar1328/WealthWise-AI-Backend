package ai.wealthwise.controller;

import ai.wealthwise.model.entity.TaxCompliance;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.EWayBill;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.SmeBusinessService;
import ai.wealthwise.service.TaxComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sme/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final TaxComplianceService taxComplianceService;
    private final SmeBusinessService smeBusinessService;
    private final UserRepository userRepository;

    @GetMapping("/{businessId}/filings")
    public ResponseEntity<List<TaxCompliance>> getAllFilings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<TaxCompliance> filings = taxComplianceService.getAllFilings(business);
        return ResponseEntity.ok(filings);
    }

    @GetMapping("/{businessId}/pending")
    public ResponseEntity<List<TaxCompliance>> getPendingFilings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<TaxCompliance> filings = taxComplianceService.getPendingFilings(business);
        return ResponseEntity.ok(filings);
    }

    @GetMapping("/{businessId}/overdue")
    public ResponseEntity<List<TaxCompliance>> getOverdueFilings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<TaxCompliance> filings = taxComplianceService.getOverdueFilings(business);
        return ResponseEntity.ok(filings);
    }

    @GetMapping("/{businessId}/upcoming")
    public ResponseEntity<List<TaxCompliance>> getUpcomingFilings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @RequestParam(defaultValue = "30") int days) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<TaxCompliance> filings = taxComplianceService.getUpcomingFilings(business, days);
        return ResponseEntity.ok(filings);
    }

    @PostMapping("/{businessId}/filings")
    public ResponseEntity<TaxCompliance> createFiling(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @RequestBody Map<String, Object> payload) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);

        TaxCompliance.FilingType filingType = TaxCompliance.FilingType.valueOf((String) payload.get("filingType"));
        String filingPeriod = (String) payload.get("filingPeriod");
        LocalDate dueDate = LocalDate.parse((String) payload.get("dueDate"));
        BigDecimal taxLiability = new BigDecimal(payload.get("taxLiability").toString());
        BigDecimal inputTaxCredit = new BigDecimal(payload.get("inputTaxCredit").toString());

        TaxCompliance filing = taxComplianceService.createFiling(
                business, filingType, filingPeriod, dueDate, taxLiability, inputTaxCredit);
        return ResponseEntity.status(HttpStatus.CREATED).body(filing);
    }

    @PostMapping("/{businessId}/filings/{filingId}/mark-filed")
    public ResponseEntity<TaxCompliance> markAsFiled(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long filingId,
            @RequestBody Map<String, Object> payload) {
        User user = getUserFromDetails(userDetails);
        smeBusinessService.getBusinessEntity(user, businessId); // Validate access

        LocalDate filedDate = LocalDate.parse((String) payload.get("filedDate"));
        String arnNumber = (String) payload.get("arnNumber");
        BigDecimal taxPaid = new BigDecimal(payload.get("taxPaid").toString());
        String filingReference = (String) payload.get("filingReference");

        TaxCompliance filing = taxComplianceService.markAsFiled(
                filingId, filedDate, arnNumber, taxPaid, filingReference);
        return ResponseEntity.ok(filing);
    }

    @PostMapping("/{businessId}/generate-schedule")
    public ResponseEntity<Map<String, String>> generateGstSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @RequestParam String fiscalYear) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        taxComplianceService.generateGstFilingSchedule(business, fiscalYear);
        return ResponseEntity.ok(Map.of("message", "GST filing schedule generated for " + fiscalYear));
    }

    @GetMapping("/{businessId}/score")
    public ResponseEntity<Map<String, Object>> getComplianceScore(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        Double score = taxComplianceService.getAverageComplianceScore(business);

        Map<String, Object> result = Map.of(
                "complianceScore", score != null ? score : 0.0,
                "pendingFilings", taxComplianceService.getPendingFilings(business).size(),
                "overdueFilings", taxComplianceService.getOverdueFilings(business).size());
        return ResponseEntity.ok(result);
    }

    // ============= NEW ENDPOINTS =============

    @PostMapping("/{businessId}/sync-gst")
    public ResponseEntity<Map<String, String>> syncGstData(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        taxComplianceService.syncGstData(business);
        return ResponseEntity.ok(Map.of("message", "GST Data Synced Successfully"));
    }

    @GetMapping("/{businessId}/eway-bills")
    public ResponseEntity<List<EWayBill>> getEWayBills(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        return ResponseEntity.ok(taxComplianceService.getEWayBills(business));
    }

    @GetMapping("/{businessId}/filings/{filingId}/validate")
    public ResponseEntity<Map<String, String>> validateReturn(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long filingId) {
        // Just checking access
        User user = getUserFromDetails(userDetails);
        smeBusinessService.getBusinessEntity(user, businessId);

        String validationMsg = taxComplianceService.validateReturn(filingId);
        return ResponseEntity.ok(Map.of("validationResult", validationMsg));
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
