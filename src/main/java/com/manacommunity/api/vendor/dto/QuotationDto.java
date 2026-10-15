package com.manacommunity.api.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationDto {
    private Long id;
    private String quotationNumber;
    private Long purchaseRequestId;
    private Long vendorId;
    private String vendorName;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private Integer deliveryDays;
    private Integer warrantyDays;
    private String paymentTerms;
    private Integer validityDays;
    private String notes;
    private Long communityId;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private List<QuotationItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotationItemDto {
        private Long id;
        private String itemName;
        private String specification;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxPercent;
        private BigDecimal discountPercent;
        private BigDecimal total;
        private Integer sortOrder;
    }
}
