package com.manacommunity.api.vendor.dto;

import com.manacommunity.api.vendor.entity.VmsSlaBreachLog.BreachStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaBreachResponseDto {
    private Long id;
    private Long contractId;
    private Long workOrderId;
    private Long vendorId;
    private Long communityId;
    private String metricName;
    private String targetValue;
    private String actualValue;
    private BigDecimal penaltyAmount;
    private BreachStatus status;
    private String remarks;
    private LocalDateTime breachTime;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
}
