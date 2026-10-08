package com.manacommunity.api.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContractSlaRequestDto {
    private String metricName;
    private String targetValue;
    private String unit;
    private BigDecimal penaltyAmount;
    private String measurementFreq;
    private Integer sortOrder;
}
