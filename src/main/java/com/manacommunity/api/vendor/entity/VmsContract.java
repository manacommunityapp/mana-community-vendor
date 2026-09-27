package com.manacommunity.api.vendor.entity;

import com.manacommunity.common.model.Community;
import com.manacommunity.common.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vms_contracts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", nullable = false, length = 30)
    private String contractNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ContractType type = ContractType.SERVICE;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", length = 30)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VmsVendor vendor;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal value = BigDecimal.ZERO;

    @Column(name = "total_value", precision = 14, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "payment_frequency", length = 20)
    private String paymentFrequency;

    @Column(name = "payment_terms", length = 1000)
    private String paymentTerms;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = false;

    @Column(name = "renewal_notice_days")
    @Builder.Default
    private Integer renewalNoticeDays = 30;

    @Column(name = "penalty_clause", length = 2000)
    private String penaltyClause;

    @Column(name = "termination_clause", length = 2000)
    private String terminationClause;

    @Column(name = "signed_by_vendor", nullable = false)
    @Builder.Default
    private Boolean signedByVendor = false;

    @Column(name = "signed_by_admin", nullable = false)
    @Builder.Default
    private Boolean signedByAdmin = false;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public ContractType getContractType() {
        return contractType != null ? contractType : type;
    }

    public BigDecimal getTotalValue() {
        return totalValue != null ? totalValue : value;
    }

    public enum ContractType { SERVICE, AMC, INSURANCE, LEASE, SUPPLY, SLA, MAINTENANCE }
    public enum ContractStatus { DRAFT, ACTIVE, EXPIRED, TERMINATED, RENEWED, PENDING_RENEWAL }
}

