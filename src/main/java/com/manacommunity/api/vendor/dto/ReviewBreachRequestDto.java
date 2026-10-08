package com.manacommunity.api.vendor.dto;

import com.manacommunity.api.vendor.entity.VmsSlaBreachLog.BreachStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBreachRequestDto {
    private BreachStatus status; // PENALTY_APPLIED or WAIVED
    private String remarks;
    private Long reviewedBy;
}
