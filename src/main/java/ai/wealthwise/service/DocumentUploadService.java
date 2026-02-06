package ai.wealthwise.service;

import ai.wealthwise.model.dto.sme.DocumentUploadResponse;
import ai.wealthwise.model.entity.FinancialDocument;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.repository.FinancialDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadService {

    private final FinancialDocumentRepository documentRepository;
    private final DocumentParsingService documentParsingService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("csv", "xlsx", "xls", "pdf", "jpg", "jpeg", "png");

    @Transactional
    public DocumentUploadResponse uploadDocument(
            SmeBusiness business,
            MultipartFile file,
            FinancialDocument.DocumentCategory category,
            String fiscalYear,
            String description) throws IOException {

        // Validate file
        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        FinancialDocument.FileType fileType = parseFileType(extension);

        // Generate unique filename
        String uniqueFileName = UUID.randomUUID().toString() + "." + extension;

        // Calculate checksum
        String checksum = calculateChecksum(file.getBytes());

        // Check for duplicate
        if (documentRepository.existsByChecksum(checksum)) {
            throw new IllegalArgumentException("This file has already been uploaded");
        }

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir, String.valueOf(business.getId()));
        Files.createDirectories(uploadPath);

        // Save file
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create document record
        FinancialDocument document = FinancialDocument.builder()
                .smeBusiness(business)
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .storagePath(filePath.toString())
                .documentCategory(category)
                .parseStatus(FinancialDocument.ParseStatus.PENDING)
                .fiscalYear(fiscalYear)
                .description(description)
                .checksum(checksum)
                .build();

        FinancialDocument saved = java.util.Objects.requireNonNull(documentRepository.save(document));
        if (saved.getId() == null) {
            throw new RuntimeException("Failed to save document record");
        }
        log.info("Document uploaded: {} for business: {}", originalFileName, business.getId());

        // Automatically trigger parsing for bank statements
        if (category == FinancialDocument.DocumentCategory.BANK_STATEMENT) {
            log.info("Auto-triggering parsing for document: {}", saved.getId());
            documentParsingService.parseDocumentAsync(saved.getId());
        }

        DocumentUploadResponse response = DocumentUploadResponse.fromEntity(saved);
        response.setMessage("Document uploaded successfully. Processing will begin shortly.");
        return response;
    }

    public List<DocumentUploadResponse> getDocuments(SmeBusiness business) {
        return documentRepository.findBySmeBusinessOrderByUploadedAtDesc(business).stream()
                .map(DocumentUploadResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<DocumentUploadResponse> getDocumentsByCategory(SmeBusiness business,
            FinancialDocument.DocumentCategory category) {
        return documentRepository.findBySmeBusinessAndDocumentCategory(business, category).stream()
                .map(DocumentUploadResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(SmeBusiness business, Long documentId) throws IOException {
        FinancialDocument document = documentRepository.findByIdAndSmeBusiness(documentId, business)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        // Delete file from filesystem
        Path filePath = Paths.get(document.getStoragePath());
        Files.deleteIfExists(filePath);

        // Delete record
        documentRepository.delete(document);
        log.info("Document deleted: {} from business: {}", documentId, business.getId());
    }

    @Transactional
    public void updateParseStatus(Long documentId, FinancialDocument.ParseStatus status, String error,
            Integer rowCount) {
        if (documentId == null)
            return;
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setParseStatus(status);
            doc.setParseError(error);
            doc.setRowCount(rowCount);
            if (status == FinancialDocument.ParseStatus.COMPLETED || status == FinancialDocument.ParseStatus.FAILED) {
                doc.setParsedAt(LocalDateTime.now());
            }
            documentRepository.save(doc);
        });
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Supported: CSV, XLSX, XLS, PDF, JPG, JPEG, PNG");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private FinancialDocument.FileType parseFileType(String extension) {
        return switch (extension.toLowerCase()) {
            case "csv" -> FinancialDocument.FileType.CSV;
            case "xlsx" -> FinancialDocument.FileType.XLSX;
            case "xls" -> FinancialDocument.FileType.XLS;
            case "pdf" -> FinancialDocument.FileType.PDF;
            case "jpg", "jpeg", "png" -> FinancialDocument.FileType.IMAGE;
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };
    }

    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
