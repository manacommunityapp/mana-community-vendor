package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.vendor.dto.CreateContractSlaRequestDto;
import com.manacommunity.api.vendor.dto.ReviewBreachRequestDto;
import com.manacommunity.api.vendor.dto.SlaBreachResponseDto;
import com.manacommunity.api.vendor.entity.VmsContractSla;
import com.manacommunity.api.vendor.service.VmsSlaMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vms/slas")
@RequiredArgsConstructor
public class VmsSlaController {

    private final VmsSlaMonitoringService slaService;

    @PostMapping("/contracts/{contractId}")
    public ResponseEntity<VmsContractSla> createContractSla(
            @PathVariable Long contractId,
            @RequestBody CreateContractSlaRequestDto dto) {
        return ResponseEntity.ok(slaService.createContractSla(contractId, dto));
    }

    @GetMapping("/contracts/{contractId}")
    public ResponseEntity<List<VmsContractSla>> getContractSlas(@PathVariable Long contractId) {
        return ResponseEntity.ok(slaService.getContractSlas(contractId));
    }

    @PostMapping("/breaches/{breachId}/review")
    public ResponseEntity<SlaBreachResponseDto> reviewBreach(
            @PathVariable Long breachId,
            @RequestParam Long communityId,
            @RequestBody ReviewBreachRequestDto dto) {
        return ResponseEntity.ok(slaService.reviewBreach(breachId, communityId, dto));
    }

    @GetMapping("/breaches")
    public ResponseEntity<Page<SlaBreachResponseDto>> getCommunityBreaches(
            @RequestParam Long communityId,
            Pageable pageable) {
        return ResponseEntity.ok(slaService.getBreachesByCommunity(communityId, pageable));
    }

    @GetMapping("/vendors/{vendorId}/breaches")
    public ResponseEntity<Page<SlaBreachResponseDto>> getVendorBreaches(
            @PathVariable Long vendorId,
            Pageable pageable) {
        return ResponseEntity.ok(slaService.getBreachesByVendor(vendorId, pageable));
    }
}
