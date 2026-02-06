package ai.wealthwise.model.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitResponse {
    private String orderId;
    private String currency;
    private Integer amount; // In paise
    private String keyId; // For frontend to initialize Checkout
    private String receiptId;
}
