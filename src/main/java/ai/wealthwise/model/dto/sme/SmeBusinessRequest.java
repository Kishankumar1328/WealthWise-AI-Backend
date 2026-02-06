package ai.wealthwise.model.dto.sme;

import ai.wealthwise.model.entity.SmeBusiness;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmeBusinessRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 200, message = "Business name must not exceed 200 characters")
    private String businessName;

    // GSTIN is optional - pattern only validated if value is present
    private String gstin;

    // PAN is optional - pattern only validated if value is present
    private String pan;

    @NotNull(message = "Industry type is required")
    private SmeBusiness.IndustryType industryType;

    @NotNull(message = "Business size is required")
    private SmeBusiness.BusinessSize businessSize;

    @DecimalMin(value = "0.0", message = "Annual turnover cannot be negative")
    private BigDecimal annualTurnover;

    private LocalDate registrationDate;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    // Pincode is optional - pattern only validated if value is present
    private String pincode;

    @Email(message = "Invalid email format")
    private String contactEmail;

    // Phone is optional - pattern only validated if value is present
    private String contactPhone;
}
