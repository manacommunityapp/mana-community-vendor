package com.manacommunity.api.vendor.service;

import com.manacommunity.api.vendor.dto.CreateContractSlaRequestDto;
import com.manacommunity.api.vendor.dto.ReviewBreachRequestDto;
import com.manacommunity.api.vendor.dto.SlaBreachResponseDto;
import com.manacommunity.api.vendor.entity.VmsContract;
import com.manacommunity.api.vendor.entity.VmsContractSla;
import com.manacommunity.api.vendor.entity.VmsSlaBreachLog;
import com.manacommunity.api.vendor.entity.VmsSlaBreachLog.BreachStatus;
import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import com.manacommunity.api.vendor.repository.VmsContractRepository;
import com.manacommunity.api.vendor.repository.VmsContractSlaRepository;
import com.manacommunity.api.vendor.repository.VmsSlaBreachLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VmsSlaMonitoringService {

    private final VmsContractSlaRepository contractSlaRepository;
    private final VmsSlaBreachLogRepository breachLogRepository;
    private final VmsContractRepository contractRepository;

    @Transactional
    public VmsContractSla createContractSla(Long contractId, CreateContractSlaRequestDto dto) {
        VmsContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        VmsContractSla sla = VmsContractSla.builder()
                .contract(contract)
                .metricName(dto.getMetricName())
                .targetValue(dto.getTargetValue())
                .unit(dto.getUnit())
                .penaltyAmount(dto.getPenaltyAmount() != null ? dto.getPenaltyAmount() : BigDecimal.ZERO)
                .measurementFreq(dto.getMeasurementFreq() != null ? dto.getMeasurementFreq() : "PER_INCIDENT")
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();

        return contractSlaRepository.save(sla);
    }

    @Transactional(readOnly = true)
    public List<VmsContractSla> getContractSlas(Long contractId) {
        return contractSlaRepository.findByContractIdOrderBySortOrderAsc(contractId);
    }

    @Transactional
    public List<VmsSlaBreachLog> evaluateWorkOrderSla(VmsWorkOrder workOrder) {
        if (workOrder.getVendor() == null || workOrder.getCommunity() == null) {
            return List.of();
        }

        Long vendorId = workOrder.getVendor().getId();
        Long communityId = workOrder.getCommunity().getId();

        List<VmsContract> contracts = contractRepository.findByVendorId(vendorId, Pageable.unpaged()).getContent();
        if (contracts.isEmpty()) {
            return List.of();
        }

        VmsContract activeContract = contracts.stream()
                .filter(c -> c.getStatus() == VmsContract.ContractStatus.ACTIVE)
                .findFirst()
                .orElse(contracts.get(0));

        List<VmsContractSla> slas = contractSlaRepository.findByContractId(activeContract.getId());
        List<VmsSlaBreachLog> breaches = new ArrayList<>();

        LocalDateTime scheduled = workOrder.getScheduledDate() != null
                ? workOrder.getScheduledDate().atStartOfDay()
                : workOrder.getCreatedAt();

        LocalDateTime completed = workOrder.getCompletedDate() != null
                ? workOrder.getCompletedDate()
                : LocalDateTime.now();

        long actualHours = Duration.between(scheduled, completed).toHours();
        if (actualHours < 0) actualHours = 0;

        for (VmsContractSla sla : slas) {
            try {
                // If SLA specifies metric e.g. "Resolution Time" with target hours
                long targetHours = Long.parseLong(sla.getTargetValue().replaceAll("[^0-9]", ""));
                if (targetHours > 0 && actualHours > targetHours) {
                    VmsSlaBreachLog breach = VmsSlaBreachLog.builder()
                            .contractId(activeContract.getId())
                            .workOrderId(workOrder.getId())
                            .vendorId(vendorId)
                            .communityId(communityId)
                            .metricName(sla.getMetricName())
                            .targetValue(sla.getTargetValue() + " (" + sla.getUnit() + ")")
                            .actualValue(actualHours + " Hours")
                            .penaltyAmount(sla.getPenaltyAmount())
                            .status(BreachStatus.PENDING_REVIEW)
                            .remarks("Turnaround exceeded SLA target by " + (actualHours - targetHours) + " hours")
                            .breachTime(LocalDateTime.now())
                            .build();

                    breaches.add(breachLogRepository.save(breach));
                }
            } catch (Exception ignored) {
                // Ignore parsing errors for non-numeric SLA targets
            }
        }

        return breaches;
    }

    @Transactional
    public SlaBreachResponseDto reviewBreach(Long breachId, Long communityId, ReviewBreachRequestDto dto) {
        VmsSlaBreachLog breach = breachLogRepository.findByIdAndCommunityId(breachId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("SLA Breach record not found: " + breachId));

        breach.setStatus(dto.getStatus());
        if (dto.getRemarks() != null) {
            breach.setRemarks(dto.getRemarks());
        }
        breach.setReviewedBy(dto.getReviewedBy());
        breach.setReviewedAt(LocalDateTime.now());

        return toDto(breachLogRepository.save(breach));
    }

    @Transactional(readOnly = true)
    public Page<SlaBreachResponseDto> getBreachesByCommunity(Long communityId, Pageable pageable) {
        return breachLogRepository.findByCommunityIdOrderByBreachTimeDesc(communityId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<SlaBreachResponseDto> getBreachesByVendor(Long vendorId, Pageable pageable) {
        return breachLogRepository.findByVendorIdOrderByBreachTimeDesc(vendorId, pageable).map(this::toDto);
    }

    private SlaBreachResponseDto toDto(VmsSlaBreachLog b) {
        return SlaBreachResponseDto.builder()
                .id(b.getId())
                .contractId(b.getContractId())
                .workOrderId(b.getWorkOrderId())
                .vendorId(b.getVendorId())
                .communityId(b.getCommunityId())
                .metricName(b.getMetricName())
                .targetValue(b.getTargetValue())
                .actualValue(b.getActualValue())
                .penaltyAmount(b.getPenaltyAmount())
                .status(b.getStatus())
                .remarks(b.getRemarks())
                .breachTime(b.getBreachTime())
                .reviewedAt(b.getReviewedAt())
                .reviewedBy(b.getReviewedBy())
                .build();
    }
}
