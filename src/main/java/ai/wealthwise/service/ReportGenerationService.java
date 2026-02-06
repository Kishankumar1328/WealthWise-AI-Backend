package ai.wealthwise.service;

import ai.wealthwise.model.entity.*;
import ai.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final FinancialReportRepository reportRepository;
    private final SmeBusinessRepository businessRepository;

    /**
     * Module 9: Async report generation.
     */
    @Transactional
    public FinancialReport initiateReportGeneration(Long businessId, FinancialReport.ReportType type, String title) {
        if (businessId == null)
            throw new IllegalArgumentException("Business ID cannot be null");

        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        FinancialReport report = FinancialReport.builder()
                .business(business)
                .reportType(type)
                .reportTitle(title)
                .status(FinancialReport.Status.GENERATING)
                .fileFormat("PDF")
                .build();

        report = java.util.Objects.requireNonNull(reportRepository.save(report));

        // Async Processing (Simulated)
        generateFileAsync(report.getId());

        return report;
    }

    private void generateFileAsync(final Long reportId) {
        if (reportId == null)
            return;

        CompletableFuture.runAsync(() -> {
            try {
                // Simulate heavy processing (PDF generation)
                Thread.sleep(2000);

                FinancialReport report = reportRepository.findById(reportId).orElseThrow();

                // In real impl: Generate PDF using iText or PDFBox
                String mockPath = "/reports/" + report.getBusiness().getId() + "/" + report.getReportType() + "_"
                        + System.currentTimeMillis() + ".pdf";

                report.setFilePath(mockPath);
                report.setFileSizeBytes(1024L * 500); // 500KB
                report.setStatus(FinancialReport.Status.COMPLETED);

                reportRepository.save(report);

            } catch (Exception e) {
                FinancialReport report = reportRepository.findById(reportId).orElse(null);
                if (report != null) {
                    report.setStatus(FinancialReport.Status.FAILED);
                    reportRepository.save(report);
                }
            }
        });
    }

    public List<FinancialReport> getBusinessReports(Long businessId) {
        if (businessId == null)
            throw new IllegalArgumentException("Business ID cannot be null");

        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        return reportRepository.findByBusinessOrderByGenerationDateDesc(business);
    }

    public byte[] getReportFile(Long businessId, Long reportId) {
        if (reportId == null || businessId == null) {
            throw new IllegalArgumentException("IDs cannot be null");
        }
        FinancialReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!java.util.Objects.equals(report.getBusiness().getId(), businessId)) {
            throw new IllegalArgumentException("Report does not belong to this business");
        }

        if (report.getStatus() != FinancialReport.Status.COMPLETED) {
            throw new RuntimeException("Report is not ready for download");
        }

        // Return a mock PDF byte array for now
        return "MOCK_PDF_CONTENT".getBytes();
    }
}
