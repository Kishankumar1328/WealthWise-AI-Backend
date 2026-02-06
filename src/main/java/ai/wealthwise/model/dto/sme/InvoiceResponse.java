package ai.wealthwise.model.dto.sme;

import ai.wealthwise.model.entity.Invoice;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private Long id;
    private Invoice.InvoiceType invoiceType;
    private String invoiceNumber;
    private String partyName;
    private String partyGstin;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Invoice.InvoiceStatus status;
    private BigDecimal paidAmount;
    private LocalDate paidDate;
    private Integer daysOverdue;
    private String description;
    private String category;
    private LocalDateTime createdAt;

    public static InvoiceResponse fromEntity(Invoice entity) {
        return InvoiceResponse.builder()
                .id(entity.getId())
                .invoiceType(entity.getInvoiceType())
                .invoiceNumber(entity.getInvoiceNumber())
                .partyName(entity.getPartyName())
                .partyGstin(entity.getPartyGstin())
                .invoiceDate(entity.getInvoiceDate())
                .dueDate(entity.getDueDate())
                .amount(entity.getAmount())
                .taxAmount(entity.getTaxAmount())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .paidAmount(entity.getPaidAmount())
                .paidDate(entity.getPaidDate())
                .daysOverdue(entity.getDaysOverdue())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
