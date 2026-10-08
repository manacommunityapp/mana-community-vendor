package com.manacommunity.api.vendor.dto;

import com.manacommunity.api.vendor.entity.VmsWorkOrderSignoff.SignoffType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverSignoffRequestDto {
    private SignoffType signoffType;
    private String signatoryName;
    private String signatoryPhone;
    private String signatureUrl;
    private String otpCode;
    private Integer rating;
    private String feedbackNotes;
    private BigDecimal actualCost;
    private String completionNotes;
}
