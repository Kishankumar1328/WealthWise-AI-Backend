package ai.wealthwise.model.dto.sme;

import ai.wealthwise.model.entity.Invoice;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    @NotNull(message = "Invoice type is required")
    private Invoice.InvoiceType invoiceType;

    @NotBlank(message = "Invoice number is required")
    @Size(max = 50)
    private String invoiceNumber;

    @NotBlank(message = "Party name is required")
    @Size(max = 200)
    private String partyName;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    private String partyGstin;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @DecimalMin(value = "0.0")
    private BigDecimal taxAmount;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String category;
}
