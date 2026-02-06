package ai.wealthwise.controller;

import ai.wealthwise.model.entity.DashboardWidget;
import ai.wealthwise.model.entity.ParsedTransaction;
import ai.wealthwise.model.entity.ScenarioAnalysis;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.ParsedTransactionRepository;
import ai.wealthwise.repository.SmeBusinessRepository;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.DashboardWidgetService;
import ai.wealthwise.service.ScenarioAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ScenarioAnalysisService scenarioService;
    private final DashboardWidgetService widgetService;
    private final UserRepository userRepository;
    private final ParsedTransactionRepository transactionRepository;
    private final SmeBusinessRepository smeBusinessRepository;

    @PostMapping("/scenarios")
    public ResponseEntity<ScenarioAnalysis> createScenario(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long businessId,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam BigDecimal revenueGrowth,
            @RequestParam BigDecimal expenseGrowth) {

        // In real app, verify user owns business
        return ResponseEntity
                .ok(scenarioService.createScenario(businessId, name, description, revenueGrowth, expenseGrowth));
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<ScenarioAnalysis>> getScenarios(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long businessId) {
        return ResponseEntity.ok(scenarioService.getScenarios(businessId));
    }

    @PostMapping("/widgets/init")
    public ResponseEntity<List<DashboardWidget>> initWidgets(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        return ResponseEntity.ok(widgetService.initDefaultWidgets(user.getId()));
    }

    @GetMapping("/widgets")
    public ResponseEntity<List<DashboardWidget>> getWidgets(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        return ResponseEntity.ok(widgetService.getUserWidgets(user.getId()));
    }

    // ========= Advanced Analytics Endpoints (safe extensions) =========

    /**
     * Overview endpoint that returns dashboard-ready summary metrics for a business
     * over a given period. All fields are always present with safe defaults so
     * that frontends never break on empty data.
     */
    // ========= Advanced Analytics Endpoints (safe extensions) =========

    /**
     * Overview endpoint that returns dashboard-ready summary metrics.
     * Uses simulation if no real transaction data exists.
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(
            @RequestParam Long businessId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (businessId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "businessId is required"));
        }

        SmeBusiness business = smeBusinessRepository.findById(businessId).orElse(null);
        if (business == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Business not found"));
        }

        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(90);

        List<ParsedTransaction> recentTx = transactionRepository.findRecentTransactions(businessId, 30);

        boolean isSimulation = recentTx.isEmpty();
        BigDecimal totalCredits, totalDebits;

        if (isSimulation) {
            // SIMULATION MODE
            totalCredits = BigDecimal.valueOf(1500000 + (Math.random() * 500000));
            totalDebits = BigDecimal.valueOf(800000 + (Math.random() * 200000));
        } else {
            BigDecimal dbCredits = transactionRepository.sumCreditsForPeriod(business, effectiveStart, effectiveEnd);
            BigDecimal dbDebits = transactionRepository.sumDebitsForPeriod(business, effectiveStart, effectiveEnd);
            totalCredits = dbCredits != null ? dbCredits : BigDecimal.ZERO;
            totalDebits = dbDebits != null ? dbDebits : BigDecimal.ZERO;
        }

        BigDecimal netCashFlow = totalCredits.subtract(totalDebits);

        // Category Breakdown Logic
        Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
        if (isSimulation) {
            categoryBreakdown.put("Operational", totalDebits.multiply(BigDecimal.valueOf(0.4)));
            categoryBreakdown.put("Payroll", totalDebits.multiply(BigDecimal.valueOf(0.35)));
            categoryBreakdown.put("Marketing", totalDebits.multiply(BigDecimal.valueOf(0.15)));
            categoryBreakdown.put("Software & Tools", totalDebits.multiply(BigDecimal.valueOf(0.1)));
        } else {
            List<Object[]> results = transactionRepository.getExpensesByCategory(business, effectiveStart,
                    effectiveEnd);
            for (Object[] row : results) {
                String cat = (String) row[0];
                BigDecimal amt = (BigDecimal) row[1];
                categoryBreakdown.put(cat != null ? cat : "Uncategorized", amt);
            }
            // Fallback if empty
            if (categoryBreakdown.isEmpty()) {
                categoryBreakdown.put("General", totalDebits);
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCredits", totalCredits);
        summary.put("totalDebits", totalDebits);
        summary.put("netCashFlow", netCashFlow);
        summary.put("transactionCount", isSimulation ? 45 : recentTx.size());
        summary.put("isSimulation", isSimulation);
        summary.put("categoryBreakdown", categoryBreakdown);

        Map<String, Object> response = new HashMap<>();
        response.put("period", Map.of("startDate", effectiveStart, "endDate", effectiveEnd));
        response.put("summary", summary);
        response.put("recentTransactions", isSimulation
                ? generateSimulatedTransactions(10)
                : recentTx.stream().limit(20).map(this::mapTransactionForOverview).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trends")
    public ResponseEntity<?> getTrends(
            @RequestParam Long businessId,
            @RequestParam(required = false, defaultValue = "NET_CASHFLOW") String metric,
            @RequestParam(required = false, defaultValue = "90") int days) {

        if (businessId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "businessId is required"));
        if (days <= 0)
            days = 90;

        List<ParsedTransaction> txs = transactionRepository.findRecentTransactions(businessId, days);
        boolean isSimulation = txs.isEmpty();

        List<Map<String, Object>> series;
        if (isSimulation) {
            series = generateSimulatedTrend(days);
        } else {
            // ... existing logic ...
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(days);
            Map<LocalDate, Map<String, BigDecimal>> buckets = new HashMap<>();
            for (ParsedTransaction tx : txs) {
                if (tx.getTransactionDate() == null)
                    continue;
                LocalDate d = tx.getTransactionDate();
                if (d.isBefore(start) || d.isAfter(end))
                    continue;

                Map<String, BigDecimal> bucket = buckets.computeIfAbsent(d, k -> {
                    Map<String, BigDecimal> m = new HashMap<>();
                    m.put("credits", BigDecimal.ZERO);
                    m.put("debits", BigDecimal.ZERO);
                    return m;
                });

                String key = tx.getTransactionType() == ParsedTransaction.TransactionType.CREDIT ? "credits" : "debits";
                bucket.put(key, bucket.get(key).add(tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO));
            }

            series = buckets.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(this::mapBucketToPoint)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(Map.of(
                "metric", metric,
                "businessId", businessId,
                "isSimulation", isSimulation,
                "points", series));
    }

    @GetMapping("/risks")
    public ResponseEntity<?> getRisks(
            @RequestParam Long businessId,
            @RequestParam(required = false, defaultValue = "90") int days) {

        if (businessId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "businessId is required"));

        List<ParsedTransaction> txs = transactionRepository.findRecentTransactions(businessId, days);
        boolean isSimulation = txs.isEmpty();

        BigDecimal net;
        String band;
        int negativeNetDays;

        if (isSimulation) {
            net = BigDecimal.valueOf(50000); // Positive simulation
            band = "LOW";
            negativeNetDays = 5;
        } else {
            // ... existing logic ...
            BigDecimal totalCredits = BigDecimal.ZERO;
            BigDecimal totalDebits = BigDecimal.ZERO;
            negativeNetDays = 0;
            Map<LocalDate, BigDecimal> dailyNet = new HashMap<>();

            for (ParsedTransaction tx : txs) {
                // Simplified loop for brevity, same logic as before
                if (tx.getTransactionDate() == null || tx.getAmount() == null)
                    continue;
                BigDecimal signAmount = tx.getTransactionType() == ParsedTransaction.TransactionType.CREDIT
                        ? tx.getAmount()
                        : tx.getAmount().negate();
                dailyNet.merge(tx.getTransactionDate(), signAmount, BigDecimal::add);
                if (tx.getTransactionType() == ParsedTransaction.TransactionType.CREDIT)
                    totalCredits = totalCredits.add(tx.getAmount());
                else
                    totalDebits = totalDebits.add(tx.getAmount());
            }

            for (BigDecimal v : dailyNet.values()) {
                if (v.compareTo(BigDecimal.ZERO) < 0)
                    negativeNetDays++;
            }
            net = totalCredits.subtract(totalDebits);
            if (net.compareTo(BigDecimal.ZERO) < 0 && negativeNetDays > days / 3)
                band = "HIGH";
            else if (net.compareTo(BigDecimal.ZERO) < 0)
                band = "MEDIUM";
            else
                band = "LOW";
        }

        Map<String, Object> liquidityRisk = new HashMap<>();
        liquidityRisk.put("riskType", "CASHFLOW");
        liquidityRisk.put("score", net);
        liquidityRisk.put("band", band);
        liquidityRisk.put("daysAnalyzed", days);
        liquidityRisk.put("negativeNetDays", negativeNetDays);

        return ResponseEntity.ok(Map.of(
                "businessId", businessId,
                "asOfDate", LocalDate.now(),
                "isSimulation", isSimulation,
                "risks", List.of(liquidityRisk)));
    }

    // --- Helper Methods ---

    private List<Map<String, Object>> generateSimulatedTransactions(int count) {
        // Return dummy transaction list map
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", (long) i);
            m.put("date", LocalDate.now().minusDays(i * 2));
            m.put("description", "Simulated Transaction " + i);
            m.put("amount", BigDecimal.valueOf(1000 + Math.random() * 5000));
            m.put("type", i % 2 == 0 ? "CREDIT" : "DEBIT");
            m.put("category", i % 2 == 0 ? "Sales" : "Operations");
            return m;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> generateSimulatedTrend(int days) {
        List<Map<String, Object>> points = new java.util.ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(days);
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            BigDecimal credits = BigDecimal.valueOf(5000 + Math.random() * 5000); // 5k-10k daily revenue
            BigDecimal debits = BigDecimal.valueOf(2000 + Math.random() * 4000); // 2k-6k daily expense

            // Add some seasonality/noise
            if (i % 7 == 0 || i % 7 == 6) { // Weekends lower
                credits = credits.multiply(BigDecimal.valueOf(0.5));
                debits = debits.multiply(BigDecimal.valueOf(0.5));
            }

            Map<String, Object> p = new HashMap<>();
            p.put("date", d);
            p.put("credits", credits);
            p.put("debits", debits);
            p.put("net", credits.subtract(debits));
            points.add(p);
        }
        return points;
    }

    private Map<String, Object> mapBucketToPoint(Map.Entry<LocalDate, Map<String, BigDecimal>> e) {
        BigDecimal credits = e.getValue().get("credits");
        BigDecimal debits = e.getValue().get("debits");
        BigDecimal net = credits.subtract(debits);
        Map<String, Object> m = new HashMap<>();
        m.put("date", e.getKey());
        m.put("credits", credits);
        m.put("debits", debits);
        m.put("net", net);
        return m;
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> mapTransactionForOverview(ParsedTransaction tx) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tx.getId());
        map.put("date", tx.getTransactionDate());
        map.put("description", tx.getDescription());
        map.put("amount", tx.getAmount());
        map.put("type", tx.getTransactionType() != null ? tx.getTransactionType().name() : null);
        map.put("category", tx.getCategory());
        return map;
    }
}
