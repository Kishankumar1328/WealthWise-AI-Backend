package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SME Business Entity - Core business profile for SME financial assessment
 */
@Entity
@Table(name = "sme_businesses", indexes = {
        @Index(name = "idx_sme_user", columnList = "user_id"),
        @Index(name = "idx_sme_gstin", columnList = "gstin")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmeBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "gstin", length = 15, unique = true)
    private String gstin;

    @Column(name = "pan", length = 10)
    private String pan;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry_type", nullable = false, length = 50)
    private IndustryType industryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_size", nullable = false, length = 20)
    private BusinessSize businessSize;

    @Column(name = "annual_turnover", precision = 15, scale = 2)
    private BigDecimal annualTurnover;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 6)
    private String pincode;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 15)
    private String contactPhone;

    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT true")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Enums ====================

    public enum IndustryType {
        MANUFACTURING,
        RETAIL,
        AGRICULTURE,
        SERVICES,
        LOGISTICS,
        ECOMMERCE,
        HEALTHCARE,
        EDUCATION,
        CONSTRUCTION,
        IT_TECHNOLOGY,
        HOSPITALITY,
        OTHER
    }

    public enum BusinessSize {
        MICRO, // Turnover < 5 Cr
        SMALL, // Turnover 5-75 Cr
        MEDIUM // Turnover 75-250 Cr
    }

    public String getSector() {
        return industryType != null ? industryType.name() : null;
    }

    public BigDecimal getAnnualRevenue() {
        return annualTurnover;
    }
}
