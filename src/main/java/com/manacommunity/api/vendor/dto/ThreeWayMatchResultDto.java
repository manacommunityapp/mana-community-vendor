package com.manacommunity.api.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreeWayMatchResultDto {
    private Long purchaseOrderId;
    private String poNumber;
    private Long goodsReceiptId;
    private String grnNumber;
    private Long invoiceId;
    private String invoiceNumber;
    private String matchStatus; // MATCHED, VARIANCE_DETECTED, REJECTED
    private BigDecimal poTotal;
    private BigDecimal grnAcceptedTotal;
    private BigDecimal invoiceTotal;
    private BigDecimal priceVariance;
    private BigDecimal priceVariancePercentage;
    private Boolean isPriceWithinTolerance;
    private Boolean isQuantityMatched;
    private List<ItemMatchDetail> itemDetails;
    private String summaryMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemMatchDetail {
        private String itemName;
        private BigDecimal orderedQty;
        private BigDecimal receivedQty;
        private BigDecimal acceptedQty;
        private BigDecimal billedQty;
        private BigDecimal unitPricePo;
        private BigDecimal unitPriceBilled;
        private Boolean isQtyMatched;
        private Boolean isPriceMatched;
        private String note;
    }
}
