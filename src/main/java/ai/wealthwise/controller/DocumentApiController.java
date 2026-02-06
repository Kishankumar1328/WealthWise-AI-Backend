package ai.wealthwise.controller;

import ai.wealthwise.model.dto.sme.DocumentUploadResponse;
import ai.wealthwise.model.entity.FinancialDocument;
import ai.wealthwise.model.entity.ParsedTransaction;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.FinancialDocumentRepository;
import ai.wealthwise.repository.ParsedTransactionRepository;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.DocumentParsingService;
import ai.wealthwise.service.DocumentUploadService;
import ai.wealthwise.service.SmeBusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thin, stable alias API for document analysis that delegates to the
 * existing SME document pipeline without modifying its behavior.
 *
 * NOTE: This controller intentionally duplicates a small amount of mapping
 * logic from {@link DocumentController} instead of refactoring shared
 * helpers, to avoid any risk of regressions in the existing SME endpoints.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentApiController {

    private final DocumentUploadService documentUploadService;
    private final DocumentParsingService documentParsingService;
    private final SmeBusinessService smeBusinessService;
    private final UserRepository userRepository;
    private final ParsedTransactionRepository transactionRepository;
    private final FinancialDocumentRepository documentRepository;

    /**
     * Generic upload endpoint that mirrors the SME upload behavior but exposes
     * a flatter URL structure as required by the API specification.
     *
     * It still requires a valid SME business owned by the authenticated user
     * and routes through the existing {@link DocumentUploadService}.
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("businessId") Long businessId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") FinancialDocument.DocumentCategory category,
            @RequestParam(value = "fiscalYear", required = false) String fiscalYear,
            @RequestParam(value = "description", required = false) String description) throws IOException {

        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);

        DocumentUploadResponse response = documentUploadService.uploadDocument(
                business, file, category, fiscalYear, description);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns the current processing status for a document, including any
     * parsed transaction count. This is a thin alias around the existing
     * status semantics used by the SME document module.
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<?> getStatus(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromDetails(userDetails);
        FinancialDocument document = getDocumentById(documentId);

        if (!document.getSmeBusiness().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        long transactionCount = transactionRepository.countByDocument(document);

        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "fileName", document.getFileName(),
                "status", document.getParseStatus().name(),
                "transactionCount", transactionCount,
                "parseError", document.getParseError() != null ? document.getParseError() : ""));
    }

    /**
     * Returns the parsed analysis payload for a document. For now this
     * exposes the same structure as the SME transactions endpoint so that
     * frontends can safely consume both without branching.
     */
    @GetMapping("/{documentId}/analysis")
    public ResponseEntity<?> getAnalysis(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromDetails(userDetails);
        FinancialDocument document = getDocumentById(documentId);

        if (!document.getSmeBusiness().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        List<ParsedTransaction> transactions = transactionRepository
                .findByDocumentOrderByTransactionDateDesc(document);

        List<Map<String, Object>> mapped = transactions.stream()
                .map(this::mapTransaction)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "transactionCount", mapped.size(),
                "transactions", mapped));
    }

    /**
     * Optional helper to trigger parsing via the flat API path; this simply
     * forwards to the existing asynchronous parsing pipeline.
     */
    @PostMapping("/{documentId}/parse")
    public ResponseEntity<?> parseDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromDetails(userDetails);
        FinancialDocument document = getDocumentById(documentId);

        if (!document.getSmeBusiness().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        documentParsingService.parseDocumentAsync(documentId);

        return ResponseEntity.accepted().body(Map.of(
                "message", "Document parsing started",
                "documentId", documentId,
                "status", "PROCESSING"));
    }

    // ========= Local helpers (intentionally duplicated from DocumentController) =========

    private Map<String, Object> mapTransaction(ParsedTransaction tx) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", tx.getId());
        map.put("date", tx.getTransactionDate());
        map.put("description", tx.getDescription());
        map.put("referenceNumber", tx.getReferenceNumber());
        map.put("type", tx.getTransactionType() != null ? tx.getTransactionType().name() : null);
        map.put("amount", tx.getAmount());
        map.put("runningBalance", tx.getRunningBalance());
        map.put("category", tx.getCategory());
        map.put("subCategory", tx.getSubCategory());
        map.put("partyName", tx.getPartyName());
        map.put("confidence", tx.getAiConfidence());
        map.put("isVerified", tx.getIsVerified());
        map.put("isTaxDeductible", tx.getIsTaxDeductible());
        map.put("gstAmount", tx.getGstAmount());
        map.put("tdsAmount", tx.getTdsAmount());
        return map;
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private FinancialDocument getDocumentById(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }
}

