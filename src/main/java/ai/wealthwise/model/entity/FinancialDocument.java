package ai.wealthwise.model.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Financial Document Entity - Stores metadata for uploaded financial documents
 */
@Entity
@Table(name = "financial_documents", indexes = {
        @Index(name = "idx_fd_sme", columnList = "sme_business_id"),
        @Index(name = "idx_fd_uploaded_at", columnList = "uploaded_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private FileType fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // in bytes

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_category", nullable = false, length = 50)
    private DocumentCategory documentCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false, length = 20)
    @Builder.Default
    private ParseStatus parseStatus = ParseStatus.PENDING;

    @Column(name = "parse_error", length = 1000)
    private String parseError;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Column(name = "row_count")
    private Integer rowCount; // For CSV/XLSX files

    @Column(name = "transaction_count")
    private Integer transactionCount; // Number of parsed transactions

    @Column(name = "fiscal_year", length = 9)
    private String fiscalYear;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "checksum", length = 64)
    private String checksum; // SHA-256 hash for integrity

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // Convenience methods
    public SmeBusiness getBusiness() {
        return smeBusiness;
    }

    public String getFilePath() {
        return storagePath;
    }

    // ==================== Enums ====================

    public enum FileType {
        CSV,
        XLSX,
        XLS,
        PDF,
        IMAGE
    }

    public enum DocumentCategory {
        BANK_STATEMENT,
        GST_RETURNS,
        FINANCIAL_STATEMENT,
        INVOICE_REGISTER,
        BALANCE_SHEET,
        PROFIT_LOSS,
        CASH_FLOW,
        TRIAL_BALANCE,
        TAX_DOCUMENTS,
        OTHER
    }

    public enum ParseStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        PARTIAL
    }
}
