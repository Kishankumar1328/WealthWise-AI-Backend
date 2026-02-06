package ai.wealthwise.model.dto.sme;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarkFiledRequest(
        LocalDate filedDate,
        String arnNumber,
        BigDecimal taxPaid,
        String filingReference) {
}
