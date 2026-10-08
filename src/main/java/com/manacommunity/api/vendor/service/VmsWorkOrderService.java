package com.manacommunity.api.vendor.service;

import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.api.vendor.dto.*;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import com.manacommunity.api.vendor.entity.VmsWorkOrderSignoff;
import com.manacommunity.api.vendor.entity.VmsWorkOrderSignoff.SignoffType;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.vendor.repository.VmsWorkOrderRepository;
import com.manacommunity.api.vendor.repository.VmsWorkOrderSignoffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmsWorkOrderService {

    private final VmsWorkOrderRepository workOrderRepo;
    private final VmsVendorRepository vendorRepo;
    private final VmsWorkOrderSignoffRepository signoffRepo;
    private final VmsSlaMonitoringService slaService;

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> getCommunityWorkOrders(Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return workOrderRepo.findByCommunityIdAndStatus(communityId, VmsWorkOrder.WorkOrderStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return workOrderRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> getVendorWorkOrders(Long vendorId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return workOrderRepo.findByVendorIdAndStatus(vendorId, VmsWorkOrder.WorkOrderStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return workOrderRepo.findByVendorId(vendorId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse getById(Long id, Long communityId) {
        return toResponse(workOrderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id)));
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest req, AppUser createdBy, Community community) {
        String woNumber = "WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        VmsWorkOrder wo = VmsWorkOrder.builder()
                .workOrderNumber(woNumber)
                .title(req.getTitle())
                .description(req.getDescription())
                .type(VmsWorkOrder.WorkOrderType.valueOf(req.getType()))
                .priority(VmsWorkOrder.Priority.valueOf(req.getPriority()))
                .location(req.getLocation())
                .scheduledDate(req.getScheduledDate())
                .estimatedCost(req.getEstimatedCost())
                .createdBy(createdBy)
                .community(community)
                .build();

        if (req.getVendorId() != null) {
            VmsVendor vendor = vendorRepo.findById(req.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));
            wo.setVendor(vendor);
            wo.setStatus(VmsWorkOrder.WorkOrderStatus.ASSIGNED);
        }

        return toResponse(workOrderRepo.save(wo));
    }

    @Transactional
    public WorkOrderResponse updateStatus(Long id, String status, Long communityId) {
        VmsWorkOrder wo = workOrderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));
        wo.setStatus(VmsWorkOrder.WorkOrderStatus.valueOf(status));
        return toResponse(workOrderRepo.save(wo));
    }

    @Transactional
    public WorkOrderResponse assignVendor(Long id, Long vendorId, Long communityId) {
        VmsWorkOrder wo = workOrderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));
        VmsVendor vendor = vendorRepo.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
        wo.setVendor(vendor);
        wo.setStatus(VmsWorkOrder.WorkOrderStatus.ASSIGNED);
        return toResponse(workOrderRepo.save(wo));
    }

    @Transactional
    public WorkOrderSignoffResponseDto generateHandoverOtp(Long workOrderId, Long communityId, HandoverOtpRequestDto req) {
        VmsWorkOrder wo = workOrderRepo.findByIdAndCommunityId(workOrderId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        VmsWorkOrderSignoff signoff = VmsWorkOrderSignoff.builder()
                .workOrderId(wo.getId())
                .communityId(communityId)
                .signoffType(SignoffType.RESIDENT_OTP)
                .signatoryPhone(req.getResidentPhone())
                .signatoryName(req.getResidentName())
                .otpCode(otp)
                .isVerified(false)
                .generatedAt(LocalDateTime.now())
                .build();

        VmsWorkOrderSignoff saved = signoffRepo.save(signoff);
        return toSignoffDto(saved);
    }

    @Transactional
    public WorkOrderResponse completeWithSignoff(Long workOrderId, Long communityId, HandoverSignoffRequestDto dto) {
        VmsWorkOrder wo = workOrderRepo.findByIdAndCommunityId(workOrderId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        if (dto.getSignoffType() == SignoffType.RESIDENT_OTP) {
            VmsWorkOrderSignoff pendingSignoff = signoffRepo
                    .findTopByWorkOrderIdAndSignoffTypeOrderByGeneratedAtDesc(workOrderId, SignoffType.RESIDENT_OTP)
                    .orElseThrow(() -> new IllegalStateException("No handover OTP generated for this work order"));

            if (dto.getOtpCode() == null || !dto.getOtpCode().trim().equals(pendingSignoff.getOtpCode())) {
                throw new IllegalArgumentException("Invalid handover OTP code");
            }

            pendingSignoff.setIsVerified(true);
            pendingSignoff.setSignedAt(LocalDateTime.now());
            pendingSignoff.setRating(dto.getRating());
            pendingSignoff.setFeedbackNotes(dto.getFeedbackNotes());
            signoffRepo.save(pendingSignoff);
        } else {
            // DIGITAL_SIGNATURE or SUPERVISOR_SIGNATURE
            VmsWorkOrderSignoff signatureSignoff = VmsWorkOrderSignoff.builder()
                    .workOrderId(wo.getId())
                    .communityId(communityId)
                    .signoffType(dto.getSignoffType() != null ? dto.getSignoffType() : SignoffType.DIGITAL_SIGNATURE)
                    .signatoryName(dto.getSignatoryName())
                    .signatureUrl(dto.getSignatureUrl())
                    .isVerified(true)
                    .rating(dto.getRating())
                    .feedbackNotes(dto.getFeedbackNotes())
                    .generatedAt(LocalDateTime.now())
                    .signedAt(LocalDateTime.now())
                    .build();
            signoffRepo.save(signatureSignoff);
        }

        wo.setStatus(VmsWorkOrder.WorkOrderStatus.COMPLETED);
        wo.setCompletedDate(LocalDateTime.now());
        wo.setCompletedAt(LocalDateTime.now());
        wo.setResidentApproved(true);
        wo.setResidentApprovedAt(LocalDateTime.now());

        if (dto.getActualCost() != null) {
            wo.setActualCost(dto.getActualCost());
        }
        if (dto.getCompletionNotes() != null) {
            wo.setCompletionNotes(dto.getCompletionNotes());
        }

        VmsWorkOrder completedWo = workOrderRepo.save(wo);

        // Evaluate SLAs automatically upon work order completion
        slaService.evaluateWorkOrderSla(completedWo);

        return toResponse(completedWo);
    }

    private WorkOrderSignoffResponseDto toSignoffDto(VmsWorkOrderSignoff s) {
        return WorkOrderSignoffResponseDto.builder()
                .id(s.getId())
                .workOrderId(s.getWorkOrderId())
                .communityId(s.getCommunityId())
                .signoffType(s.getSignoffType())
                .signatoryName(s.getSignatoryName())
                .signatoryPhone(s.getSignatoryPhone())
                .signatureUrl(s.getSignatureUrl())
                .isVerified(s.getIsVerified())
                .rating(s.getRating())
                .feedbackNotes(s.getFeedbackNotes())
                .generatedAt(s.getGeneratedAt())
                .signedAt(s.getSignedAt())
                .build();
    }

    private WorkOrderResponse toResponse(VmsWorkOrder wo) {
        return WorkOrderResponse.builder()
                .id(wo.getId())
                .workOrderNumber(wo.getWorkOrderNumber())
                .title(wo.getTitle())
                .description(wo.getDescription())
                .type(wo.getType() != null ? wo.getType().name() : null)
                .priority(wo.getPriority() != null ? wo.getPriority().name() : null)
                .status(wo.getStatus() != null ? wo.getStatus().name() : null)
                .vendor(wo.getVendor() != null ? WorkOrderResponse.VendorRef.builder()
                        .id(wo.getVendor().getId())
                        .businessName(wo.getVendor().getBusinessName())
                        .build() : null)
                .location(wo.getLocation())
                .scheduledDate(wo.getScheduledDate())
                .completedDate(wo.getCompletedDate() != null ? wo.getCompletedDate().toLocalDate() : null)
                .estimatedCost(wo.getEstimatedCost())
                .actualCost(wo.getActualCost())
                .completionNotes(wo.getCompletionNotes())
                .communityId(wo.getCommunity() != null ? wo.getCommunity().getId() : null)
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }
}
