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

@RestController
@RequestMapping("/api/v1/sme/documents")
@RequiredArgsConstructor
public class DocumentController {

        private final DocumentUploadService documentUploadService;
        private final DocumentParsingService documentParsingService;
        private final SmeBusinessService smeBusinessService;
        private final UserRepository userRepository;
        private final ParsedTransactionRepository transactionRepository;
        private final FinancialDocumentRepository documentRepository;

        @PostMapping("/{businessId}/upload")
        public ResponseEntity<DocumentUploadResponse> uploadDocument(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long businessId,
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

        @GetMapping("/{businessId}")
        public ResponseEntity<List<DocumentUploadResponse>> getDocuments(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long businessId) {
                User user = getUserFromDetails(userDetails);
                SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
                List<DocumentUploadResponse> documents = documentUploadService.getDocuments(business);
                return ResponseEntity.ok(documents);
        }

        @GetMapping("/{businessId}/category/{category}")
        public ResponseEntity<List<DocumentUploadResponse>> getDocumentsByCategory(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long businessId,
                        @PathVariable FinancialDocument.DocumentCategory category) {
                User user = getUserFromDetails(userDetails);
                SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
                List<DocumentUploadResponse> documents = documentUploadService.getDocumentsByCategory(business,
                                category);
                return ResponseEntity.ok(documents);
        }

        @DeleteMapping("/{businessId}/{documentId}")
        public ResponseEntity<Map<String, String>> deleteDocument(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long businessId,
                        @PathVariable Long documentId) throws IOException {
                User user = getUserFromDetails(userDetails);
                SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
                documentUploadService.deleteDocument(business, documentId);
                return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        }

        @GetMapping("/{businessId}/{documentId}/download")
        public ResponseEntity<org.springframework.core.io.Resource> downloadDocument(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable Long businessId,
                        @PathVariable Long documentId) throws java.net.MalformedURLException {

                User user = getUserFromDetails(userDetails);
                FinancialDocument document = getDocumentById(documentId);

                if (!document.getSmeBusiness().getId().equals(businessId) ||
                                !document.getSmeBusiness().getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).build();
                }

                java.nio.file.Path filePath = java.nio.file.Paths.get(document.getStoragePath());
                java.net.URI uri = filePath.toUri();
                if (uri == null)
                        throw new RuntimeException("URI is null");
                org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(uri);

                if (resource.exists() || resource.isReadable()) {
                        return ResponseEntity.ok()
                                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\"" + document.getOriginalFileName()
                                                                        + "\"")
                                        .body(resource);
                } else {
                        throw new RuntimeException("Could not read the file!");
                }
        }

        // ================== PARSING ENDPOINTS ==================

        @PostMapping("/{documentId}/parse")
        public ResponseEntity<?> parseDocument(
                        @PathVariable Long documentId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                User user = getUserFromDetails(userDetails);
                FinancialDocument document = getDocumentById(documentId);

                // Verify ownership
                if (!document.getSmeBusiness().getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
                }

                // Trigger async parsing
                documentParsingService.parseDocumentAsync(documentId);

                return ResponseEntity.accepted().body(Map.of(
                                "message", "Document parsing started",
                                "documentId", documentId,
                                "status", "PROCESSING"));
        }

        @GetMapping("/{documentId}/status")
        public ResponseEntity<?> getParsingStatus(
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

        @GetMapping("/{documentId}/transactions")
        public ResponseEntity<?> getDocumentTransactions(
                        @PathVariable Long documentId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                User user = getUserFromDetails(userDetails);
                FinancialDocument document = getDocumentById(documentId);

                if (!document.getSmeBusiness().getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
                }

                List<ParsedTransaction> transactions = transactionRepository
                                .findByDocumentOrderByTransactionDateDesc(document);

                return ResponseEntity.ok(Map.of(
                                "documentId", documentId,
                                "transactionCount", transactions.size(),
                                "transactions",
                                transactions.stream().map(this::mapTransaction)
                                                .collect(java.util.stream.Collectors.toList())));
        }

        @GetMapping("/business/{businessId}/transactions")
        public ResponseEntity<?> getBusinessTransactions(
                        @PathVariable Long businessId,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String type,
                        @AuthenticationPrincipal UserDetails userDetails) {

                User user = getUserFromDetails(userDetails);
                SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);

                List<ParsedTransaction> transactions;

                if (startDate != null && endDate != null) {
                        transactions = transactionRepository
                                        .findByBusinessAndTransactionDateBetweenOrderByTransactionDateDesc(
                                                        business, startDate, endDate);
                } else if (category != null && !category.isEmpty()) {
                        transactions = transactionRepository.findByBusinessAndCategoryOrderByTransactionDateDesc(
                                        business, category);
                } else if (type != null && !type.isEmpty()) {
                        ParsedTransaction.TransactionType txType = ParsedTransaction.TransactionType
                                        .valueOf(type.toUpperCase());
                        transactions = transactionRepository.findByBusinessAndTransactionTypeOrderByTransactionDateDesc(
                                        business, txType);
                } else {
                        transactions = transactionRepository.findByBusinessOrderByTransactionDateDesc(business);
                }

                return ResponseEntity.ok(Map.of(
                                "businessId", businessId,
                                "transactionCount", transactions.size(),
                                "transactions",
                                transactions.stream().map(this::mapTransaction)
                                                .collect(java.util.stream.Collectors.toList())));
        }

        @GetMapping("/business/{businessId}/analytics")
        public ResponseEntity<?> getBusinessAnalytics(
                        @PathVariable Long businessId,
                        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                        @AuthenticationPrincipal UserDetails userDetails) {

                User user = getUserFromDetails(userDetails);
                SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);

                java.math.BigDecimal totalCredits = transactionRepository.sumCreditsForPeriod(business, startDate,
                                endDate);
                java.math.BigDecimal totalDebits = transactionRepository.sumDebitsForPeriod(business, startDate,
                                endDate);

                List<Object[]> expensesByCategory = transactionRepository.getExpensesByCategory(business, startDate,
                                endDate);
                List<Map<String, Object>> categoryBreakdown = expensesByCategory.stream()
                                .map(row -> {
                                        Map<String, Object> item = new java.util.HashMap<>();
                                        item.put("category", row[0] != null ? row[0] : "Uncategorized");
                                        item.put("amount", row[1] != null ? row[1] : java.math.BigDecimal.ZERO);
                                        return item;
                                })
                                .collect(java.util.stream.Collectors.toList());

                List<Object[]> topVendorsData = transactionRepository.getTopVendors(business, startDate, endDate);
                List<Map<String, Object>> topVendors = topVendorsData.stream()
                                .limit(10)
                                .map(row -> {
                                        Map<String, Object> item = new java.util.HashMap<>();
                                        item.put("name", row[0] != null ? row[0] : "Unknown");
                                        item.put("totalAmount", row[1] != null ? row[1] : java.math.BigDecimal.ZERO);
                                        item.put("transactionCount", row[2] != null ? ((Number) row[2]).intValue() : 0);
                                        return item;
                                })
                                .collect(java.util.stream.Collectors.toList());

                List<Object[]> topCustomersData = transactionRepository.getTopCustomers(business, startDate, endDate);
                List<Map<String, Object>> topCustomers = topCustomersData.stream()
                                .limit(10)
                                .map(row -> {
                                        Map<String, Object> item = new java.util.HashMap<>();
                                        item.put("name", row[0] != null ? row[0] : "Unknown");
                                        item.put("totalAmount", row[1] != null ? row[1] : java.math.BigDecimal.ZERO);
                                        item.put("transactionCount", row[2] != null ? ((Number) row[2]).intValue() : 0);
                                        return item;
                                })
                                .collect(java.util.stream.Collectors.toList());

                return ResponseEntity.ok(Map.of(
                                "period", Map.of("startDate", startDate, "endDate", endDate),
                                "summary", Map.of(
                                                "totalCredits",
                                                totalCredits != null ? totalCredits : java.math.BigDecimal.ZERO,
                                                "totalDebits",
                                                totalDebits != null ? totalDebits : java.math.BigDecimal.ZERO,
                                                "netCashFlow",
                                                (totalCredits != null ? totalCredits : java.math.BigDecimal.ZERO)
                                                                .subtract(totalDebits != null ? totalDebits
                                                                                : java.math.BigDecimal.ZERO)),
                                "expensesByCategory", categoryBreakdown,
                                "topVendors", topVendors,
                                "topCustomers", topCustomers));
        }

        @GetMapping("/categories")
        public ResponseEntity<?> getCategories() {
                return ResponseEntity.ok(Map.of(
                                "categories", java.util.List.of(
                                                Map.of("name", "Salary", "subCategories",
                                                                java.util.List.of("Employee Wages",
                                                                                "Contractor Payments", "Bonuses")),
                                                Map.of("name", "Utilities", "subCategories",
                                                                java.util.List.of("Electricity", "Water", "Internet",
                                                                                "Phone")),
                                                Map.of("name", "Rent", "subCategories",
                                                                java.util.List.of("Office Rent", "Warehouse Rent",
                                                                                "Equipment Lease")),
                                                Map.of("name", "Taxes", "subCategories",
                                                                java.util.List.of("GST Payment", "TDS Payment",
                                                                                "Income Tax", "Professional Tax")),
                                                Map.of("name", "Bank Charges", "subCategories",
                                                                java.util.List.of("Service Fees", "Transaction Fees",
                                                                                "Interest Charges")),
                                                Map.of("name", "Interest", "subCategories",
                                                                java.util.List.of("Interest Earned", "Interest Paid",
                                                                                "Loan Interest")),
                                                Map.of("name", "Purchases", "subCategories",
                                                                java.util.List.of("Raw Materials", "Inventory",
                                                                                "Office Supplies", "Equipment")),
                                                Map.of("name", "Sales", "subCategories",
                                                                java.util.List.of("Product Sales", "Service Revenue",
                                                                                "Customer Receipt")),
                                                Map.of("name", "Travel", "subCategories",
                                                                java.util.List.of("Local Travel", "Business Travel",
                                                                                "Fuel", "Accommodation")),
                                                Map.of("name", "Marketing", "subCategories",
                                                                java.util.List.of("Advertising", "Digital Marketing",
                                                                                "Events", "Promotions")),
                                                Map.of("name", "Professional Services", "subCategories",
                                                                java.util.List.of("Legal", "Accounting", "Consulting")),
                                                Map.of("name", "Insurance", "subCategories",
                                                                java.util.List.of("Health", "Property", "Liability",
                                                                                "Vehicle")),
                                                Map.of("name", "Maintenance", "subCategories",
                                                                java.util.List.of("Office Maintenance",
                                                                                "Equipment Repair", "IT Support")),
                                                Map.of("name", "Income", "subCategories",
                                                                java.util.List.of("Investment Income", "Other Income",
                                                                                "Refunds")),
                                                Map.of("name", "Expenses", "subCategories",
                                                                java.util.List.of("Miscellaneous",
                                                                                "Other Expenses")))));
        }

        private Map<String, Object> mapTransaction(ParsedTransaction tx) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", tx.getId());
                map.put("date", tx.getTransactionDate());
                map.put("description", tx.getDescription());
                map.put("referenceNumber", tx.getReferenceNumber());
                map.put("type", tx.getTransactionType().name());
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
                if (documentId == null)
                        throw new IllegalArgumentException("Document ID cannot be null");
                return documentRepository.findById(documentId)
                                .orElseThrow(() -> new RuntimeException("Document not found"));
        }
}
