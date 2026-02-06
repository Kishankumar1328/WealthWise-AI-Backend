package ai.wealthwise.service;

import ai.wealthwise.model.entity.*;
import ai.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookkeepingService {

    private final BookkeepingRuleRepository ruleRepository;
    private final ParsedTransactionRepository transactionRepository;
    private final SmeBusinessRepository businessRepository;

    /**
     * Module 6: Auto-categorize transactions based on user-defined rules.
     */
    @Transactional
    public int runAutoCategorization(Long businessId) {
        if (businessId == null)
            throw new IllegalArgumentException("Business ID cannot be null");

        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        List<BookkeepingRule> rules = ruleRepository.findByBusinessAndIsActiveTrueOrderByPriorityDesc(business);
        List<ParsedTransaction> unverifiedTransactions = transactionRepository
                .findByBusinessAndIsVerifiedFalse(business);

        int updatedCount = 0;

        for (ParsedTransaction tx : unverifiedTransactions) {
            boolean matched = false;
            for (BookkeepingRule rule : rules) {
                if (matchesRule(tx, rule)) {
                    tx.setCategory(rule.getTargetCategory());
                    tx.setSubCategory(rule.getTargetSubCategory());
                    tx.setAiConfidence(1.0); // 100% confidence for user rules
                    tx.setIsVerified(true); // Auto-verify
                    matched = true;
                    break;
                }
            }
            if (matched)
                updatedCount++;
        }

        transactionRepository.saveAll(java.util.Objects.requireNonNull(unverifiedTransactions));
        return updatedCount;
    }

    /**
     * Module 6: Detect potential duplicate transactions.
     * Logic: Same Amount + Same Party + Within 24 hours
     */
    @Transactional
    public int scanForDuplicates(Long businessId) {
        if (businessId == null)
            throw new IllegalArgumentException("Business ID cannot be null");

        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        List<ParsedTransaction> recentTransactions = transactionRepository.findRecentTransactions(businessId, 60); // Last
                                                                                                                   // 60
                                                                                                                   // days
        int duplicateCount = 0;

        for (int i = 0; i < recentTransactions.size(); i++) {
            ParsedTransaction tx1 = recentTransactions.get(i);
            if (tx1.getIsPossibleDuplicate())
                continue;

            for (int j = i + 1; j < recentTransactions.size(); j++) {
                ParsedTransaction tx2 = recentTransactions.get(j);
                if (tx2.getIsPossibleDuplicate())
                    continue;

                if (arePotentialDuplicates(tx1, tx2)) {
                    String groupId = java.util.UUID.randomUUID().toString();
                    tx1.setIsPossibleDuplicate(true);
                    tx1.setDuplicateGroupId(groupId);

                    tx2.setIsPossibleDuplicate(true);
                    tx2.setDuplicateGroupId(groupId);

                    duplicateCount += 2;
                }
            }
        }

        transactionRepository.saveAll(recentTransactions);
        return duplicateCount;
    }

    // --- Helper Methods ---

    private boolean matchesRule(ParsedTransaction tx, BookkeepingRule rule) {
        // Keyword Match
        if (rule.getKeywordPattern() != null && !rule.getKeywordPattern().isEmpty()) {
            String textToSearch = (tx.getDescription() + " " + tx.getPartyName()).toLowerCase();
            if (!textToSearch.contains(rule.getKeywordPattern().toLowerCase())) {
                return false;
            }
        }

        // Amount Range Match
        if (rule.getAmountMin() != null && tx.getAmount().compareTo(rule.getAmountMin()) < 0)
            return false;
        if (rule.getAmountMax() != null && tx.getAmount().compareTo(rule.getAmountMax()) > 0)
            return false;

        // Transaction Type Match
        if (rule.getTransactionType() != null && !rule.getTransactionType().equals(tx.getTransactionType()))
            return false;

        return true;
    }

    private boolean arePotentialDuplicates(ParsedTransaction tx1, ParsedTransaction tx2) {
        // 1. Amount Match
        if (tx1.getAmount().compareTo(tx2.getAmount()) != 0)
            return false;

        // 2. Date Proximity (within 24 hours)
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(tx1.getTransactionDate(), tx2.getTransactionDate()));
        if (daysDiff > 1)
            return false;

        // 3. Description/Party Similiarity
        String desc1 = (tx1.getDescription() != null) ? tx1.getDescription().toLowerCase() : "";
        String desc2 = (tx2.getDescription() != null) ? tx2.getDescription().toLowerCase() : "";

        // Exact match or high similarity
        return desc1.equals(desc2);
    }

    @Transactional
    public BookkeepingRule createRule(Long businessId, BookkeepingRule rule) {
        if (businessId == null)
            throw new IllegalArgumentException("Business ID cannot be null");

        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        rule.setBusiness(business);
        return ruleRepository.save(rule);
    }

    public List<BookkeepingRule> getRules(Long businessId) {
        if (businessId == null)
            throw new IllegalArgumentException("Business ID cannot be null");

        SmeBusiness business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        return ruleRepository.findByBusinessOrderByPriorityDesc(business);
    }

    @Transactional
    public void deleteRule(Long businessId, Long ruleId) {
        if (ruleId == null || businessId == null) {
            throw new IllegalArgumentException("IDs cannot be null");
        }
        BookkeepingRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found"));

        if (!java.util.Objects.equals(rule.getBusiness().getId(), businessId)) {
            throw new IllegalArgumentException("Rule does not belong to this business");
        }

        ruleRepository.delete(rule);
    }
}
