package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.vendor.dto.HandoverOtpRequestDto;
import com.manacommunity.api.vendor.dto.HandoverSignoffRequestDto;
import com.manacommunity.api.vendor.dto.WorkOrderResponse;
import com.manacommunity.api.vendor.dto.WorkOrderSignoffResponseDto;
import com.manacommunity.api.vendor.service.VmsWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vms/work-orders")
@RequiredArgsConstructor
public class VmsWorkOrderSignoffController {

    private final VmsWorkOrderService workOrderService;

    @PostMapping("/{id}/handover-otp")
    public ResponseEntity<WorkOrderSignoffResponseDto> generateHandoverOtp(
            @PathVariable Long id,
            @RequestParam Long communityId,
            @RequestBody HandoverOtpRequestDto dto) {
        return ResponseEntity.ok(workOrderService.generateHandoverOtp(id, communityId, dto));
    }

    @PostMapping("/{id}/sign-off")
    public ResponseEntity<WorkOrderResponse> completeWithSignoff(
            @PathVariable Long id,
            @RequestParam Long communityId,
            @RequestBody HandoverSignoffRequestDto dto) {
        return ResponseEntity.ok(workOrderService.completeWithSignoff(id, communityId, dto));
    }
}
