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
public class GoodsReceiptDto {
    private Long id;
    private String grnNumber;
    private Long purchaseOrderId;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private String status;
    private LocalDate receivedDate;
    private Long receivedByUserId;
    private String receivedByUserName;
    private String notes;
    private Long communityId;
    private LocalDateTime createdAt;
    private List<GoodsReceiptItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoodsReceiptItemDto {
        private Long id;
        private Long poItemId;
        private String itemName;
        private BigDecimal orderedQuantity;
        private BigDecimal receivedQuantity;
        private BigDecimal acceptedQuantity;
        private BigDecimal rejectedQuantity;
        private String rejectionReason;
        private Integer sortOrder;
    }
}
