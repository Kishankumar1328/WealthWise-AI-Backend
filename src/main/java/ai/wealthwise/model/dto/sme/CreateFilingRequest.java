package ai.wealthwise.model.dto.sme;

import ai.wealthwise.model.entity.TaxCompliance;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFilingRequest(
        TaxCompliance.FilingType filingType,
        String filingPeriod,
        LocalDate dueDate,
        BigDecimal taxLiability,
        BigDecimal inputTaxCredit) {
}
