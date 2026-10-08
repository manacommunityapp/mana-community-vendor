package com.manacommunity.api.vendor.dto;

import com.manacommunity.api.vendor.entity.VmsWorkOrderSignoff.SignoffType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderSignoffResponseDto {
    private Long id;
    private Long workOrderId;
    private Long communityId;
    private SignoffType signoffType;
    private String signatoryName;
    private String signatoryPhone;
    private String signatureUrl;
    private Boolean isVerified;
    private Integer rating;
    private String feedbackNotes;
    private LocalDateTime generatedAt;
    private LocalDateTime signedAt;
}
