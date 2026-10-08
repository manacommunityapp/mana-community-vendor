package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vms_sla_breach_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsSlaBreachLog {

    public enum BreachStatus {
        PENDING_REVIEW,
        PENALTY_APPLIED,
        WAIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "metric_name", nullable = false, length = 150)
    private String metricName;

    @Column(name = "target_value", nullable = false, length = 100)
    private String targetValue;

    @Column(name = "actual_value", nullable = false, length = 100)
    private String actualValue;

    @Column(name = "penalty_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BreachStatus status = BreachStatus.PENDING_REVIEW;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "breach_time", nullable = false)
    private LocalDateTime breachTime;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @PrePersist
    void onCreate() {
        if (breachTime == null) {
            breachTime = LocalDateTime.now();
        }
    }
}
