package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "eway_bills")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EWayBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    @Column(name = "eway_bill_number", nullable = false, unique = true)
    private String ewayBillNumber;

    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

    @Column(name = "valid_upto", nullable = false)
    private LocalDateTime validUpto;

    @Column(name = "status", nullable = false)
    private String status; // ACTIVE, CANCELLED, EXPIRED

    @Column(name = "consignor_gstin")
    private String consignorGstin;

    @Column(name = "consignee_gstin")
    private String consigneeGstin;

    @Column(name = "item_value", precision = 15, scale = 2)
    private BigDecimal itemValue;

    @Column(name = "tax_value", precision = 15, scale = 2)
    private BigDecimal taxValue;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
