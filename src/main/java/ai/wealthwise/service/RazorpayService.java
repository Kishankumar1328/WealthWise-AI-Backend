package ai.wealthwise.service;

import ai.wealthwise.model.dto.payment.PaymentInitRequest;
import ai.wealthwise.model.dto.payment.PaymentInitResponse;
import ai.wealthwise.model.dto.payment.PaymentVerifyRequest;
import ai.wealthwise.model.entity.PaymentTransaction;
import ai.wealthwise.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Transactional
    public PaymentInitResponse createOrder(PaymentInitRequest request) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        // Convert amount to paise (multiply by 100)
        BigDecimal amountInPaise = request.getAmount().multiply(new BigDecimal("100"));
        orderRequest.put("amount", amountInPaise.intValue());
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt",
                request.getReceiptId() != null ? request.getReceiptId() : "txn_" + UUID.randomUUID().toString());

        Order order = razorpayClient.orders.create(orderRequest);

        // Save initial transaction state
        PaymentTransaction transaction = PaymentTransaction.builder()
                .razorpayOrderId(order.get("id"))
                .amount(request.getAmount())
                .currency(order.get("currency"))
                .status(PaymentTransaction.PaymentStatus.CREATED)
                .receiptId(order.get("receipt"))
                .description(request.getDescription())
                .build();

        paymentTransactionRepository.save(transaction);

        return PaymentInitResponse.builder()
                .orderId(order.get("id"))
                .currency(order.get("currency"))
                .amount(order.get("amount"))
                .keyId(keyId)
                .receiptId(order.get("receipt"))
                .build();
    }

    @Transactional
    public boolean verifyPayment(PaymentVerifyRequest request) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", request.getRazorpayOrderId());
        options.put("razorpay_payment_id", request.getRazorpayPaymentId());
        options.put("razorpay_signature", request.getRazorpaySignature());

        boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

        // Update transaction status
        PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(
                        () -> new RuntimeException("Transaction not found for order: " + request.getRazorpayOrderId()));

        if (isValid) {
            transaction.setStatus(PaymentTransaction.PaymentStatus.PAID);
            transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        } else {
            transaction.setStatus(PaymentTransaction.PaymentStatus.FAILED);
            transaction.setFailureReason("Invalid Signature");
        }

        paymentTransactionRepository.save(transaction);

        return isValid;
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> getAllTransactions() {
        return paymentTransactionRepository.findAll(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }
}
