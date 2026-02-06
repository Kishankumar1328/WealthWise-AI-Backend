package ai.wealthwise.controller;

import ai.wealthwise.model.dto.payment.PaymentInitRequest;
import ai.wealthwise.model.dto.payment.PaymentInitResponse;
import ai.wealthwise.model.dto.payment.PaymentVerifyRequest;
import ai.wealthwise.service.RazorpayService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@CrossOrigin
public class PaymentController {

    private final RazorpayService razorpayService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody PaymentInitRequest request) {
        try {
            PaymentInitResponse response = razorpayService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            return ResponseEntity.badRequest().body("Error creating order: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerifyRequest request) {
        try {
            boolean isValid = razorpayService.verifyPayment(request);
            if (isValid) {
                return ResponseEntity.ok("Payment verification successful");
            } else {
                return ResponseEntity.badRequest().body("Payment verification failed: Invalid signature");
            }
        } catch (RazorpayException e) {
            return ResponseEntity.badRequest().body("Error verifying payment: " + e.getMessage());
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions() {
        return ResponseEntity.ok(razorpayService.getAllTransactions());
    }
}
