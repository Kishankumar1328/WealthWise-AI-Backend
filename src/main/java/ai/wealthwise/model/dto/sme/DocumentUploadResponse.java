package ai.wealthwise.model.dto.sme;

import ai.wealthwise.model.entity.FinancialDocument;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private FinancialDocument.FileType fileType;
    private Long fileSize;
    private FinancialDocument.DocumentCategory documentCategory;
    private FinancialDocument.ParseStatus parseStatus;
    private String parseError;
    private LocalDateTime parsedAt;
    private Integer rowCount;
    private String fiscalYear;
    private LocalDateTime uploadedAt;
    private String message;

    public static DocumentUploadResponse fromEntity(FinancialDocument entity) {
        return DocumentUploadResponse.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .originalFileName(entity.getOriginalFileName())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .documentCategory(entity.getDocumentCategory())
                .parseStatus(entity.getParseStatus())
                .parseError(entity.getParseError())
                .parsedAt(entity.getParsedAt())
                .rowCount(entity.getRowCount())
                .fiscalYear(entity.getFiscalYear())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}
