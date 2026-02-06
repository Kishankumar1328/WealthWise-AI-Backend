package ai.wealthwise.controller;

import ai.wealthwise.model.dto.sme.InvoiceRequest;
import ai.wealthwise.model.dto.sme.InvoiceResponse;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.InvoiceService;
import ai.wealthwise.service.SmeBusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sme/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final SmeBusinessService smeBusinessService;
    private final UserRepository userRepository;

    @PostMapping("/{businessId}")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @Valid @RequestBody InvoiceRequest request) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        InvoiceResponse response = invoiceService.createInvoice(business, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{businessId}")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<InvoiceResponse> invoices = invoiceService.getAllInvoices(business);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{businessId}/receivables")
    public ResponseEntity<List<InvoiceResponse>> getReceivables(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<InvoiceResponse> receivables = invoiceService.getReceivables(business);
        return ResponseEntity.ok(receivables);
    }

    @GetMapping("/{businessId}/payables")
    public ResponseEntity<List<InvoiceResponse>> getPayables(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<InvoiceResponse> payables = invoiceService.getPayables(business);
        return ResponseEntity.ok(payables);
    }

    @GetMapping("/{businessId}/overdue")
    public ResponseEntity<List<InvoiceResponse>> getOverdueInvoices(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        List<InvoiceResponse> overdue = invoiceService.getOverdueInvoices(business);
        return ResponseEntity.ok(overdue);
    }

    @PostMapping("/{businessId}/{invoiceId}/pay")
    public ResponseEntity<InvoiceResponse> markAsPaid(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long invoiceId,
            @RequestBody Map<String, BigDecimal> payload) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        BigDecimal amount = payload.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        InvoiceResponse response = invoiceService.markAsPaid(business, invoiceId, amount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{businessId}/summary")
    public ResponseEntity<Map<String, Object>> getInvoiceSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);

        invoiceService.updateOverdueStatus(business);

        Map<String, Object> summary = Map.of(
                "totalReceivables", invoiceService.getTotalOutstandingReceivables(business),
                "totalPayables", invoiceService.getTotalOutstandingPayables(business),
                "overdueReceivables", invoiceService.getOverdueReceivablesAmount(business));
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/{businessId}/{invoiceId}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceRequest request) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        InvoiceResponse response = invoiceService.updateInvoice(business, invoiceId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{businessId}/{invoiceId}")
    public ResponseEntity<Void> deleteInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long invoiceId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        invoiceService.deleteInvoice(business, invoiceId);
        return ResponseEntity.noContent().build();
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
