package ai.wealthwise.service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import ai.wealthwise.model.entity.FinancialDocument;
import ai.wealthwise.model.entity.ParsedTransaction;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.repository.FinancialDocumentRepository;
import ai.wealthwise.repository.ParsedTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for parsing financial documents (PDF, Excel, CSV)
 * and extracting transactions with AI-based categorization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentParsingService {

    private final ParsedTransactionRepository parsedTransactionRepository;
    private final FinancialDocumentRepository documentRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"));

    // Common bank statement patterns
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:Rs\\.?|₹)?\\s*(-?[\\d,]+(?:\\.\\d+)?)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}[\\s-][A-Za-z]{3}[\\s-]\\d{2,4})");

    private static final Pattern UPI_PATTERN = Pattern.compile(
            "UPI[/-]([A-Za-z0-9@._-]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern NEFT_PATTERN = Pattern.compile(
            "NEFT[/-]([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern IMPS_PATTERN = Pattern.compile(
            "IMPS[/-]([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Main entry point to parse a document asynchronously
     */
    @Async
    @Transactional
    public void parseDocumentAsync(Long documentId) {
        if (documentId == null)
            return;
        try {
            FinancialDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

            updateDocumentStatus(document, FinancialDocument.ParseStatus.PROCESSING, null);

            List<ParsedTransaction> transactions = parseDocument(document);

            if (!transactions.isEmpty()) {
                // Categorize transactions using AI
                categorizeTransactionsWithAI(transactions, document.getBusiness());

                // Save all transactions
                parsedTransactionRepository.saveAll(transactions);

                document.setTransactionCount(transactions.size());
                updateDocumentStatus(document, FinancialDocument.ParseStatus.COMPLETED, null);

                log.info("Successfully parsed {} transactions from document {}",
                        transactions.size(), document.getFileName());
            } else {
                updateDocumentStatus(document, FinancialDocument.ParseStatus.FAILED,
                        "No transactions found in document. Please check the file format.");
                log.warn("No transactions found in document {}", document.getFileName());
            }

        } catch (Exception e) {
            log.error("Error parsing document {}: {}", documentId, e.getMessage(), e);
            try {
                if (documentId != null) {
                    // Re-fetch to ensure we have attached entity or id
                    documentRepository.findById(documentId)
                            .ifPresent(doc -> updateDocumentStatus(doc, FinancialDocument.ParseStatus.FAILED,
                                    e.getMessage()));
                }
            } catch (Exception ex) {
                log.error("Error updating document status", ex);
            }
        }
    }

    /**
     * Parse a document based on its file type
     */
    public List<ParsedTransaction> parseDocument(FinancialDocument document) throws Exception {
        String fileName = document.getFileName().toLowerCase();
        // The storagePath is already a relative path from the project root OR an
        // absolute path
        Path filePath = Paths.get(document.getStoragePath());

        if (!Files.exists(filePath)) {
            // Try relative to uploadDir if not found
            filePath = Paths.get(uploadDir, document.getFilePath());
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File not found: " + document.getStoragePath());
            }
        }

        if (fileName.endsWith(".pdf")) {
            return parsePDF(filePath, document);
        } else if (fileName.endsWith(".xlsx")) {
            return parseExcel(filePath, document, true);
        } else if (fileName.endsWith(".xls")) {
            return parseExcel(filePath, document, false);
        } else if (fileName.endsWith(".csv")) {
            return parseCSV(filePath, document);
        } else {
            throw new UnsupportedOperationException("Unsupported file type: " + fileName);
        }
    }

    /**
     * Parse PDF document (bank statements, invoices)
     */
    private List<ParsedTransaction> parsePDF(Path filePath, FinancialDocument document) throws Exception {
        List<ParsedTransaction> transactions = new ArrayList<>();

        try (PDDocument pdf = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(pdf);

            String[] lines = text.split("\\r?\\n");

            // Strategy 1: Look for transaction rows (Date + Description + Amount)
            transactions = parseTransactionRows(lines, document);

            // Strategy 2: If no transactions found, try Financial Statement parsing
            // (Balance Sheet/P&L)
            if (transactions.isEmpty()) {
                transactions = parseFinancialStatementRows(lines, document);
            }
        }
        return transactions;
    }

    private List<ParsedTransaction> parseTransactionRows(String[] lines, FinancialDocument document) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        int rowNumber = 0;
        ParsedTransaction currentTx = null;
        StringBuilder descriptionBuffer = new StringBuilder();

        for (String line : lines) {
            rowNumber++;
            if (line == null || line.trim().isEmpty() || isHeaderOrFooterLine(line))
                continue;

            LocalDate date = extractDate(line);
            BigDecimal[] amounts = extractAmounts(line);

            if (date != null) {
                // New transaction started
                if (currentTx != null && currentTx.getAmount() != null) {
                    currentTx.setDescription(descriptionBuffer.toString().trim());
                    transactions.add(currentTx);
                }

                descriptionBuffer = new StringBuilder(cleanDescription(line));
                String ref = extractReferenceNumber(line);
                String party = extractPartyName(line);

                BigDecimal amount = null;
                ParsedTransaction.TransactionType type = null;

                if (amounts[0] != null || amounts[1] != null) {
                    type = amounts[1] != null ? ParsedTransaction.TransactionType.CREDIT
                            : ParsedTransaction.TransactionType.DEBIT;
                    amount = amounts[1] != null ? amounts[1] : amounts[0];
                }

                currentTx = ParsedTransaction.builder()
                        .document(document)
                        .business(document.getBusiness())
                        .transactionDate(date)
                        .referenceNumber(ref)
                        .transactionType(type)
                        .amount(amount)
                        .runningBalance(amounts[2])
                        .partyName(party)
                        .sourceRowNumber(rowNumber)
                        .rawText(line.length() > 1000 ? line.substring(0, 1000) : line)
                        .isVerified(false)
                        .build();
            } else if (currentTx != null) {
                // Continuing previous transaction
                String cleanLine = cleanDescription(line);
                if (!cleanLine.isEmpty()) {
                    descriptionBuffer.append(" ").append(cleanLine);
                }
                if (currentTx.getAmount() == null && (amounts[0] != null || amounts[1] != null)) {
                    currentTx.setTransactionType(amounts[1] != null ? ParsedTransaction.TransactionType.CREDIT
                            : ParsedTransaction.TransactionType.DEBIT);
                    currentTx.setAmount(amounts[1] != null ? amounts[1] : amounts[0]);
                    currentTx.setRunningBalance(amounts[2]);
                }
            }
        }
        if (currentTx != null && currentTx.getAmount() != null) {
            currentTx.setDescription(descriptionBuffer.toString().trim());
            transactions.add(currentTx);
        }
        return transactions;
    }

    private List<ParsedTransaction> parseFinancialStatementRows(String[] lines, FinancialDocument document) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        // 1. Try to find a global date (e.g., "As of 31st March 2024")
        LocalDate globalDate = null;
        for (String line : lines) {
            LocalDate found = extractDate(line);
            if (found != null) {
                globalDate = found;
                break;
            }
        }
        // Fallback to upload date if no date found in document
        if (globalDate == null) {
            globalDate = LocalDate.now();
        }

        int rowNumber = 0;
        for (String line : lines) {
            rowNumber++;
            if (line == null || line.trim().isEmpty() || isHeaderOrFooterLine(line))
                continue;

            // Look for "Description ... Amount" pattern
            // Must have at least one valid amount and some text
            BigDecimal[] amounts = extractAmounts(line);
            BigDecimal amount = amounts[1] != null ? amounts[1] : amounts[0]; // Credit or Debit column

            // Heuristic: If we found an amount, and the line has enough text to be a
            // description
            if (amount != null && line.trim().length() > 5) {
                String description = cleanDescription(line);

                // Skip lines that are just numbers
                if (description.replaceAll("[^a-zA-Z]", "").length() < 3)
                    continue;

                ParsedTransaction tx = ParsedTransaction.builder()
                        .document(document)
                        .business(document.getBusiness())
                        .transactionDate(globalDate)
                        .description(description)
                        // Assume DEBIT for assets/expenses (positive value usually), CREDIT for
                        // liabilities/income
                        // This is a guess, AI categorization will fix the specific category later
                        .transactionType(ParsedTransaction.TransactionType.DEBIT)
                        .amount(amount)
                        .sourceRowNumber(rowNumber)
                        .rawText(line.length() > 1000 ? line.substring(0, 1000) : line)
                        .isVerified(false)
                        .build();

                transactions.add(tx);
            }
        }
        return transactions;
    }

    /**
     * Parse Excel file (XLSX or XLS)
     */
    private List<ParsedTransaction> parseExcel(Path filePath, FinancialDocument document, boolean isXlsx)
            throws Exception {
        List<ParsedTransaction> transactions = new ArrayList<>();

        try (InputStream is = Files.newInputStream(filePath);
                Workbook workbook = isXlsx ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Detect header row
            int headerRow = detectHeaderRow(sheet);
            Map<String, Integer> columnMap = mapColumns(sheet.getRow(headerRow));

            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                ParsedTransaction tx = parseExcelRow(row, columnMap, document, i);
                if (tx != null) {
                    transactions.add(tx);
                }
            }
        }

        return transactions;
    }

    /**
     * Parse CSV file
     */
    private List<ParsedTransaction> parseCSV(Path filePath, FinancialDocument document)
            throws IOException, CsvException {
        List<ParsedTransaction> transactions = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty())
                return transactions;

            // First row is usually header
            Map<String, Integer> columnMap = mapCSVColumns(rows.get(0));

            for (int i = 1; i < rows.size(); i++) {
                ParsedTransaction tx = parseCSVRow(rows.get(i), columnMap, document, i);
                if (tx != null) {
                    transactions.add(tx);
                }
            }
        }

        return transactions;
    }

    /**
     * Parse Excel row
     */
    private ParsedTransaction parseExcelRow(Row row, Map<String, Integer> columnMap,
            FinancialDocument document, int rowNumber) {
        try {
            LocalDate date = getDateFromCell(row, columnMap.get("date"));
            if (date == null)
                return null;

            String description = getStringFromCell(row, columnMap.get("description"));
            BigDecimal debit = getAmountFromCell(row, columnMap.get("debit"));
            BigDecimal credit = getAmountFromCell(row, columnMap.get("credit"));
            BigDecimal balance = getAmountFromCell(row, columnMap.get("balance"));

            if (debit == null && credit == null)
                return null;

            ParsedTransaction.TransactionType type = credit != null && credit.compareTo(BigDecimal.ZERO) > 0
                    ? ParsedTransaction.TransactionType.CREDIT
                    : ParsedTransaction.TransactionType.DEBIT;
            BigDecimal amount = type == ParsedTransaction.TransactionType.CREDIT ? credit : debit;

            return ParsedTransaction.builder()
                    .document(document)
                    .business(document.getBusiness())
                    .transactionDate(date)
                    .description(description)
                    .transactionType(type)
                    .amount(amount)
                    .runningBalance(balance)
                    .sourceRowNumber(rowNumber)
                    .isVerified(false)
                    .build();

        } catch (Exception e) {
            log.debug("Error parsing row {}: {}", rowNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Parse CSV row
     */
    private ParsedTransaction parseCSVRow(String[] row, Map<String, Integer> columnMap,
            FinancialDocument document, int rowNumber) {
        try {
            Integer dateCol = columnMap.get("date");
            if (dateCol == null || dateCol >= row.length)
                return null;

            LocalDate date = parseDate(row[dateCol]);
            if (date == null)
                return null;

            Integer descCol = columnMap.get("description");
            String description = descCol != null && descCol < row.length ? row[descCol] : "";

            Integer debitCol = columnMap.get("debit");
            Integer creditCol = columnMap.get("credit");
            Integer balanceCol = columnMap.get("balance");

            BigDecimal debit = debitCol != null && debitCol < row.length ? parseAmount(row[debitCol]) : null;
            BigDecimal credit = creditCol != null && creditCol < row.length ? parseAmount(row[creditCol]) : null;
            BigDecimal balance = balanceCol != null && balanceCol < row.length ? parseAmount(row[balanceCol]) : null;

            if (debit == null && credit == null)
                return null;

            ParsedTransaction.TransactionType type = credit != null && credit.compareTo(BigDecimal.ZERO) > 0
                    ? ParsedTransaction.TransactionType.CREDIT
                    : ParsedTransaction.TransactionType.DEBIT;
            BigDecimal amount = type == ParsedTransaction.TransactionType.CREDIT ? credit : debit;

            return ParsedTransaction.builder()
                    .document(document)
                    .business(document.getBusiness())
                    .transactionDate(date)
                    .description(description)
                    .transactionType(type)
                    .amount(amount)
                    .runningBalance(balance)
                    .sourceRowNumber(rowNumber)
                    .isVerified(false)
                    .build();

        } catch (Exception e) {
            log.debug("Error parsing CSV row {}: {}", rowNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Categorize transactions using AI service
     */
    public void categorizeTransactionsWithAI(List<ParsedTransaction> transactions, SmeBusiness business) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        
        try {
            // Batch transactions for AI categorization
            List<Map<String, Object>> txData = new ArrayList<>();
            for (ParsedTransaction tx : transactions) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", transactions.indexOf(tx));
                data.put("description", tx.getDescription());
                data.put("amount", tx.getAmount());
                data.put("type", tx.getTransactionType().name());
                data.put("party_name", tx.getPartyName());
                txData.add(data);
            }

            Map<String, Object> request = new HashMap<>();
            request.put("transactions", txData);
            request.put("industry", business.getIndustryType() != null ? business.getIndustryType().name() : "OTHER");
            request.put("business_name", business.getBusinessName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        aiServiceUrl + "/categorize-transactions",
                        HttpMethod.POST,
                        entity,
                        Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("categories");
                    if (results != null) {
                        for (Map<String, Object> result : results) {
                            Object idObj = result.get("id");
                            if (idObj == null)
                                continue;
                            int index = ((Number) idObj).intValue();
                            if (index < transactions.size()) {
                                ParsedTransaction tx = transactions.get(index);
                                tx.setCategory((String) result.get("category"));
                                tx.setSubCategory((String) result.get("sub_category"));
                                Object confObj = result.get("confidence");
                                tx.setAiConfidence(confObj != null ? ((Number) confObj).doubleValue() : 0.5);
                                tx.setIsTaxDeductible((Boolean) result.getOrDefault("is_tax_deductible", false));
                            }
                        }
                    }
                    log.debug("Successfully categorized {} transactions using AI service", transactions.size());
                    return;
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.debug("AI categorization endpoint not available ({}), using rule-based categorization", e.getStatusCode());
            } catch (org.springframework.web.client.RestClientException e) {
                log.debug("AI service unavailable for categorization, using rule-based categorization: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.debug("Error preparing AI categorization request: {}", e.getMessage());
        }
        
        // Fall back to rule-based categorization
        log.info("Using rule-based categorization for {} transactions", transactions.size());
        for (ParsedTransaction tx : transactions) {
            categorizeWithRules(tx);
        }
    }

    /**
     * Rule-based categorization fallback
     */
    private void categorizeWithRules(ParsedTransaction tx) {
        String desc = tx.getDescription() != null ? tx.getDescription().toLowerCase() : "";

        // Salary patterns
        if (desc.contains("salary") || desc.contains("payroll") || desc.contains("wages")) {
            tx.setCategory("Salary");
            tx.setSubCategory("Employee Wages");
            tx.setIsTaxDeductible(true);
        }
        // Utility patterns
        else if (desc.contains("electricity") || desc.contains("power") || desc.contains("bescom") ||
                desc.contains("msedcl")) {
            tx.setCategory("Utilities");
            tx.setSubCategory("Electricity");
            tx.setIsTaxDeductible(true);
        } else if (desc.contains("water") || desc.contains("bwssb")) {
            tx.setCategory("Utilities");
            tx.setSubCategory("Water");
            tx.setIsTaxDeductible(true);
        }
        // Rent patterns
        else if (desc.contains("rent") || desc.contains("lease")) {
            tx.setCategory("Rent");
            tx.setSubCategory("Office Rent");
            tx.setIsTaxDeductible(true);
        }
        // Tax patterns
        else if (desc.contains("gst") || desc.contains("tax") || desc.contains("tds")) {
            tx.setCategory("Taxes");
            tx.setSubCategory("GST/Tax Payment");
        }
        // Bank charges
        else if (desc.contains("bank charge") || desc.contains("transaction fee") ||
                desc.contains("sms alert")) {
            tx.setCategory("Bank Charges");
            tx.setSubCategory("Service Fees");
            tx.setIsTaxDeductible(true);
        }
        // Interest
        else if (desc.contains("interest") || desc.contains("int.")) {
            tx.setCategory("Interest");
            tx.setSubCategory(tx.getTransactionType() == ParsedTransaction.TransactionType.CREDIT ? "Interest Earned"
                    : "Interest Paid");
        }
        // Vendor payments
        else if (desc.contains("vendor") || desc.contains("supplier") || desc.contains("purchase")) {
            tx.setCategory("Purchases");
            tx.setSubCategory("Vendor Payment");
            tx.setIsTaxDeductible(true);
        }
        // Customer receipts
        else if (desc.contains("customer") || desc.contains("sale") || desc.contains("invoice")) {
            tx.setCategory("Sales");
            tx.setSubCategory("Customer Receipt");
        }
        // Default
        else {
            if (tx.getTransactionType() == ParsedTransaction.TransactionType.CREDIT) {
                tx.setCategory("Income");
                tx.setSubCategory("Other Income");
            } else {
                tx.setCategory("Expenses");
                tx.setSubCategory("Other Expenses");
                tx.setIsTaxDeductible(true);
            }
        }

        tx.setAiConfidence(0.6); // Lower confidence for rule-based
    }

    // ================== HELPER METHODS ==================

    private void updateDocumentStatus(FinancialDocument document, FinancialDocument.ParseStatus status, String error) {
        document.setParseStatus(status);
        if (error != null) {
            document.setParseError(error);
        }
        documentRepository.save(document);
    }

    private boolean isHeaderOrFooterLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("date") && lower.contains("description") ||
                lower.contains("opening balance") ||
                lower.contains("closing balance") ||
                lower.contains("page") ||
                lower.contains("statement") && lower.contains("account") ||
                line.trim().length() < 10;
    }

    private LocalDate extractDate(String line) {
        // Try common date patterns
        Matcher matcher = DATE_PATTERN.matcher(line);

        if (matcher.find()) {
            return parseDate(matcher.group(1));
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty())
            return null;

        dateStr = dateStr.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        return null;
    }

    private BigDecimal[] extractAmounts(String line) {
        BigDecimal debit = null;
        BigDecimal credit = null;
        BigDecimal balance = null;

        Matcher matcher = AMOUNT_PATTERN.matcher(line);
        List<BigDecimal> amounts = new ArrayList<>();

        while (matcher.find()) {
            try {
                String amountStr = matcher.group(1).replaceAll(",", "");
                BigDecimal amount = new BigDecimal(amountStr);
                amounts.add(amount);
            } catch (NumberFormatException e) {
                // Skip
            }
        }

        // Logical deduction for bank statements (Date | Description | Chq | Value Date
        // | Withdrawal | Deposit | Balance)
        if (amounts.size() >= 3) {
            // Assume format: [..., Amount1, Amount2, Balance]
            BigDecimal last = amounts.get(amounts.size() - 1);
            BigDecimal secondToLast = amounts.get(amounts.size() - 2);
            BigDecimal thirdToLast = amounts.get(amounts.size() - 3);

            balance = last;
            String lowerLine = line.toLowerCase();

            // Priority 1: Check for explicit markers
            if (lowerLine.contains("cr") || lowerLine.contains("deposit") || lowerLine.contains("credit")) {
                credit = secondToLast.compareTo(BigDecimal.ZERO) != 0 ? secondToLast : thirdToLast;
            } else if (lowerLine.contains("dr") || lowerLine.contains("withdrawal") || lowerLine.contains("debit")) {
                debit = secondToLast.compareTo(BigDecimal.ZERO) != 0 ? secondToLast : thirdToLast;
            } else {
                // Priority 2: Heuristic based on non-zero values
                if (secondToLast.compareTo(BigDecimal.ZERO) != 0) {
                    credit = secondToLast;
                } else if (thirdToLast.compareTo(BigDecimal.ZERO) != 0) {
                    debit = thirdToLast;
                } else {
                    debit = secondToLast;
                }
            }
        } else if (amounts.size() == 2) {
            balance = amounts.get(1);
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("cr") || lowerLine.contains("deposit") || lowerLine.contains("credit")) {
                credit = amounts.get(0);
            } else {
                debit = amounts.get(0);
            }
        }

        return new BigDecimal[] { debit, credit, balance };
    }

    private String extractReferenceNumber(String line) {
        // Try UPI reference
        Matcher upiMatcher = UPI_PATTERN.matcher(line);
        if (upiMatcher.find())
            return "UPI-" + upiMatcher.group(1);

        // Try NEFT reference
        Matcher neftMatcher = NEFT_PATTERN.matcher(line);
        if (neftMatcher.find())
            return "NEFT-" + neftMatcher.group(1);

        // Try IMPS reference
        Matcher impsMatcher = IMPS_PATTERN.matcher(line);
        if (impsMatcher.find())
            return "IMPS-" + impsMatcher.group(1);

        return null;
    }

    private String cleanDescription(String line) {
        // Remove date patterns
        String cleaned = DATE_PATTERN.matcher(line).replaceAll("");
        // Remove amount patterns
        cleaned = AMOUNT_PATTERN.matcher(cleaned).replaceAll("");
        // Remove extra spaces and common noise
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
    }

    private String extractPartyName(String line) {
        // Extract party name from UPI/NEFT descriptions
        Pattern partyPattern = Pattern.compile(
                "(?:TO|FROM|BY|VIA)\\s+([A-Z][A-Za-z\\s]+?)(?:\\s+[A-Z]{4,}|\\d|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = partyPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private int detectHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            for (Cell cell : row) {
                String value = getCellValueAsString(cell).toLowerCase();
                if (value.contains("date") || value.contains("description") ||
                        value.contains("debit") || value.contains("credit")) {
                    return i;
                }
            }
        }
        return 0;
    }

    private Map<String, Integer> mapColumns(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        if (headerRow == null)
            return columnMap;

        for (Cell cell : headerRow) {
            String value = getCellValueAsString(cell).toLowerCase();
            int col = cell.getColumnIndex();

            if (value.contains("date") && !columnMap.containsKey("date")) {
                columnMap.put("date", col);
            } else if ((value.contains("description") || value.contains("narration") ||
                    value.contains("particulars")) && !columnMap.containsKey("description")) {
                columnMap.put("description", col);
            } else if ((value.contains("debit") || value.contains("withdrawal") ||
                    value.contains("dr")) && !columnMap.containsKey("debit")) {
                columnMap.put("debit", col);
            } else if ((value.contains("credit") || value.contains("deposit") ||
                    value.contains("cr")) && !columnMap.containsKey("credit")) {
                columnMap.put("credit", col);
            } else if (value.contains("balance") && !columnMap.containsKey("balance")) {
                columnMap.put("balance", col);
            }
        }

        return columnMap;
    }

    private Map<String, Integer> mapCSVColumns(String[] header) {
        Map<String, Integer> columnMap = new HashMap<>();

        for (int i = 0; i < header.length; i++) {
            String value = header[i].toLowerCase().trim();

            if (value.contains("date") && !columnMap.containsKey("date")) {
                columnMap.put("date", i);
            } else if ((value.contains("description") || value.contains("narration") ||
                    value.contains("particulars")) && !columnMap.containsKey("description")) {
                columnMap.put("description", i);
            } else if ((value.contains("debit") || value.contains("withdrawal") ||
                    value.contains("dr")) && !columnMap.containsKey("debit")) {
                columnMap.put("debit", i);
            } else if ((value.contains("credit") || value.contains("deposit") ||
                    value.contains("cr")) && !columnMap.containsKey("credit")) {
                columnMap.put("credit", i);
            } else if (value.contains("balance") && !columnMap.containsKey("balance")) {
                columnMap.put("balance", i);
            }
        }

        return columnMap;
    }

    private LocalDate getDateFromCell(Row row, Integer colIndex) {
        if (colIndex == null)
            return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null)
            return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return parseDate(getCellValueAsString(cell));
    }

    private String getStringFromCell(Row row, Integer colIndex) {
        if (colIndex == null)
            return "";
        Cell cell = row.getCell(colIndex);
        return cell != null ? getCellValueAsString(cell) : "";
    }

    private BigDecimal getAmountFromCell(Row row, Integer colIndex) {
        if (colIndex == null)
            return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null)
            return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            return value != 0 ? BigDecimal.valueOf(value) : null;
        }
        return parseAmount(getCellValueAsString(cell));
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty())
            return null;
        try {
            String cleaned = amountStr.replaceAll("[^\\d.-]", "");
            if (cleaned.isEmpty())
                return null;
            BigDecimal amount = new BigDecimal(cleaned);
            return amount.compareTo(BigDecimal.ZERO) != 0 ? amount.abs() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
