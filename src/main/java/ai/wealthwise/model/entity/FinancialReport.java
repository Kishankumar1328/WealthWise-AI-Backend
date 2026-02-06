package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity for Generated Financial Reports
 * Module 9: Investor-Ready Reports
 */
@Entity
@Table(name = "financial_reports", indexes = {
        @Index(name = "idx_report_business", columnList = "business_id"),
        @Index(name = "idx_report_type", columnList = "report_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private SmeBusiness business;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "report_title", nullable = false)
    private String reportTitle;

    @Column(name = "generation_date", nullable = false)
    private LocalDateTime generationDate;

    @Column(name = "file_path")
    private String filePath; // Local path or S3 URL

    @Column(name = "file_format")
    @Builder.Default
    private String fileFormat = "PDF"; // PDF, EXCEL, CSV

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson; // Date range, specific filters used

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.GENERATING;

    public enum ReportType {
        MIS_REPORT,
        INVESTOR_PITCH_DECK,
        AUDIT_PACKAGE,
        TAX_FILING_SUMMARY,
        CASH_FLOW_ANALYSIS
    }

    public enum Status {
        GENERATING,
        COMPLETED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        if (generationDate == null) {
            generationDate = LocalDateTime.now();
        }
    }
}
