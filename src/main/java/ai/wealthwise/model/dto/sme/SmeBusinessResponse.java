package ai.wealthwise.model.dto.sme;

import ai.wealthwise.model.entity.SmeBusiness;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmeBusinessResponse {
    private Long id;
    private String businessName;
    private String gstin;
    private String pan;
    private SmeBusiness.IndustryType industryType;
    private SmeBusiness.BusinessSize businessSize;
    private BigDecimal annualTurnover;
    private LocalDate registrationDate;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String contactEmail;
    private String contactPhone;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static SmeBusinessResponse fromEntity(SmeBusiness entity) {
        return SmeBusinessResponse.builder()
                .id(entity.getId())
                .businessName(entity.getBusinessName())
                .gstin(entity.getGstin())
                .pan(entity.getPan())
                .industryType(entity.getIndustryType())
                .businessSize(entity.getBusinessSize())
                .annualTurnover(entity.getAnnualTurnover())
                .registrationDate(entity.getRegistrationDate())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .state(entity.getState())
                .pincode(entity.getPincode())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
