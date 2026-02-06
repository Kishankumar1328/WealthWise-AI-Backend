package ai.wealthwise.controller;

import ai.wealthwise.model.entity.FinancialReport;
import ai.wealthwise.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sme/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportGenerationService reportService;

    @GetMapping("/{businessId}")
    public ResponseEntity<List<FinancialReport>> getReports(@PathVariable Long businessId) {
        return ResponseEntity.ok(reportService.getBusinessReports(businessId));
    }

    @PostMapping("/{businessId}/generate")
    public ResponseEntity<FinancialReport> generateReport(
            @PathVariable Long businessId,
            @RequestParam FinancialReport.ReportType type,
            @RequestParam String title) {
        return ResponseEntity.ok(reportService.initiateReportGeneration(businessId, type, title));
    }

    @GetMapping("/{businessId}/{reportId}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long businessId, @PathVariable Long reportId) {
        byte[] content = reportService.getReportFile(businessId, reportId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=report_" + reportId + ".pdf")
                .body(content);
    }
}
