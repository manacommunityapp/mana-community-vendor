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
public class PurchaseOrderDto {
    private Long id;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private Long quotationId;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private LocalDate deliveryDate;
    private String deliveryAddress;
    private String paymentTerms;
    private String termsConditions;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private Long communityId;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<PurchaseOrderItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderItemDto {
        private Long id;
        private String itemName;
        private String specification;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxPercent;
        private BigDecimal discountPercent;
        private BigDecimal total;
        private BigDecimal receivedQuantity;
        private Integer sortOrder;
    }
}
