package com.manacommunity.api.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDto {
    private Long id;
    private String requestNumber;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDate requiredByDate;
    private String department;
    private BigDecimal totalEstimatedCost;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private Long communityId;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<PurchaseRequestItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseRequestItemDto {
        private Long id;
        private String itemName;
        private String specification;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal estimatedPrice;
        private Integer sortOrder;
    }
}
