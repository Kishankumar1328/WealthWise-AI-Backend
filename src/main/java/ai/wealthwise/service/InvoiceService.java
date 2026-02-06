package ai.wealthwise.service;

import ai.wealthwise.exception.ResourceNotFoundException;
import ai.wealthwise.model.dto.sme.InvoiceRequest;
import ai.wealthwise.model.dto.sme.InvoiceResponse;
import ai.wealthwise.model.entity.Invoice;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public InvoiceResponse createInvoice(SmeBusiness business, InvoiceRequest request) {
        BigDecimal taxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = request.getAmount().add(taxAmount);

        Invoice invoice = Invoice.builder()
                .smeBusiness(business)
                .invoiceType(request.getInvoiceType())
                .invoiceNumber(request.getInvoiceNumber())
                .partyName(request.getPartyName())
                .partyGstin(request.getPartyGstin())
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .amount(request.getAmount())
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .status(Invoice.InvoiceStatus.PENDING)
                .paidAmount(BigDecimal.ZERO)
                .description(request.getDescription())
                .category(request.getCategory())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created: {} for business: {}", saved.getInvoiceNumber(), business.getId());
        return InvoiceResponse.fromEntity(saved);
    }

    public List<InvoiceResponse> getAllInvoices(SmeBusiness business) {
        return invoiceRepository.findBySmeBusinessOrderByDueDateDesc(business).stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InvoiceResponse> getReceivables(SmeBusiness business) {
        return invoiceRepository.findBySmeBusinessAndInvoiceType(business, Invoice.InvoiceType.RECEIVABLE).stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InvoiceResponse> getPayables(SmeBusiness business) {
        return invoiceRepository.findBySmeBusinessAndInvoiceType(business, Invoice.InvoiceType.PAYABLE).stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InvoiceResponse> getOverdueInvoices(SmeBusiness business) {
        updateOverdueStatus(business);
        return invoiceRepository.findBySmeBusinessAndStatus(business, Invoice.InvoiceStatus.OVERDUE).stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse markAsPaid(SmeBusiness business, Long invoiceId, BigDecimal amountPaid) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getSmeBusiness().getId().equals(business.getId())) {
            throw new IllegalArgumentException("Invoice does not belong to this business");
        }

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(amountPaid);
        invoice.setPaidAmount(newPaidAmount);

        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
            invoice.setPaidDate(LocalDate.now());
            invoice.setDaysOverdue(0);
        } else {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }

        return InvoiceResponse.fromEntity(invoiceRepository.save(invoice));
    }

    @Transactional
    public void updateOverdueStatus(SmeBusiness business) {
        LocalDate today = LocalDate.now();
        List<Invoice> pendingInvoices = invoiceRepository.findPendingByType(business, Invoice.InvoiceType.RECEIVABLE);
        pendingInvoices.addAll(invoiceRepository.findPendingByType(business, Invoice.InvoiceType.PAYABLE));

        for (Invoice invoice : pendingInvoices) {
            if (invoice.getDueDate().isBefore(today) && invoice.getStatus() != Invoice.InvoiceStatus.OVERDUE) {
                invoice.setStatus(Invoice.InvoiceStatus.OVERDUE);
                invoice.setDaysOverdue((int) ChronoUnit.DAYS.between(invoice.getDueDate(), today));
                invoiceRepository.save(invoice);
            }
        }
    }

    public BigDecimal getTotalOutstandingReceivables(SmeBusiness business) {
        BigDecimal total = invoiceRepository.sumOutstandingByType(business, Invoice.InvoiceType.RECEIVABLE);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalOutstandingPayables(SmeBusiness business) {
        BigDecimal total = invoiceRepository.sumOutstandingByType(business, Invoice.InvoiceType.PAYABLE);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getOverdueReceivablesAmount(SmeBusiness business) {
        BigDecimal total = invoiceRepository.sumOverdueReceivables(business, LocalDate.now());
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public InvoiceResponse updateInvoice(SmeBusiness business, Long invoiceId, InvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getSmeBusiness().getId().equals(business.getId())) {
            throw new IllegalArgumentException("Invoice does not belong to this business");
        }

        BigDecimal taxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = request.getAmount().add(taxAmount);

        invoice.setInvoiceType(request.getInvoiceType());
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setPartyName(request.getPartyName());
        invoice.setPartyGstin(request.getPartyGstin());
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setAmount(request.getAmount());
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setDescription(request.getDescription());
        invoice.setCategory(request.getCategory());

        log.info("Invoice updated: {} for business: {}", invoice.getInvoiceNumber(), business.getId());
        return InvoiceResponse.fromEntity(invoiceRepository.save(invoice));
    }

    @Transactional
    public void deleteInvoice(SmeBusiness business, Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getSmeBusiness().getId().equals(business.getId())) {
            throw new IllegalArgumentException("Invoice does not belong to this business");
        }

        invoiceRepository.delete(invoice);
        log.info("Invoice deleted: {} for business: {}", invoiceId, business.getId());
    }
}
