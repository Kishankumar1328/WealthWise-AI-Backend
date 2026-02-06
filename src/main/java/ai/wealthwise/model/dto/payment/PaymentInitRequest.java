package ai.wealthwise.model.dto.payment;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentInitRequest {
    private BigDecimal amount; // Amount in smallest currency unit (e.g., paise for INR)? No, usually API takes
                               // normal, we convert. Lets handle it.
    // Actually Razorpay expects amount in paise (integer). But robust APIs usually
    // take decimal or strictly documented.
    // Let's take BigDecimal and convert in service.
    private String currency = "INR";
    private String receiptId;
    private String description;
}
