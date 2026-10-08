package com.manacommunity.api.vendor.service;

import com.manacommunity.api.vendor.dto.HandoverOtpRequestDto;
import com.manacommunity.api.vendor.dto.HandoverSignoffRequestDto;
import com.manacommunity.api.vendor.dto.WorkOrderResponse;
import com.manacommunity.api.vendor.dto.WorkOrderSignoffResponseDto;
import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import com.manacommunity.api.vendor.entity.VmsWorkOrderSignoff;
import com.manacommunity.api.vendor.entity.VmsWorkOrderSignoff.SignoffType;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.vendor.repository.VmsWorkOrderRepository;
import com.manacommunity.api.vendor.repository.VmsWorkOrderSignoffRepository;
import com.manacommunity.common.model.Community;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VmsWorkOrderServiceTest {

    @Mock
    private VmsWorkOrderRepository workOrderRepo;

    @Mock
    private VmsVendorRepository vendorRepo;

    @Mock
    private VmsWorkOrderSignoffRepository signoffRepo;

    @Mock
    private VmsSlaMonitoringService slaService;

    @InjectMocks
    private VmsWorkOrderService workOrderService;

    @Test
    void testGenerateHandoverOtp_Success() {
        VmsWorkOrder wo = VmsWorkOrder.builder().id(10L).build();
        when(workOrderRepo.findByIdAndCommunityId(10L, 1L)).thenReturn(Optional.of(wo));
        when(signoffRepo.save(any(VmsWorkOrderSignoff.class))).thenAnswer(i -> {
            VmsWorkOrderSignoff s = i.getArgument(0);
            s.setId(100L);
            return s;
        });

        HandoverOtpRequestDto req = HandoverOtpRequestDto.builder()
                .residentPhone("9876543210")
                .residentName("Resident John")
                .build();

        WorkOrderSignoffResponseDto response = workOrderService.generateHandoverOtp(10L, 1L, req);

        assertNotNull(response);
        assertEquals(SignoffType.RESIDENT_OTP, response.getSignoffType());
        assertEquals("Resident John", response.getSignatoryName());
        verify(signoffRepo).save(any(VmsWorkOrderSignoff.class));
    }

    @Test
    void testCompleteWithSignoff_ValidOtp_CompletesWorkOrder() {
        Community community = Community.builder().id(1L).build();
        VmsWorkOrder wo = VmsWorkOrder.builder()
                .id(10L)
                .community(community)
                .status(VmsWorkOrder.WorkOrderStatus.IN_PROGRESS)
                .build();

        VmsWorkOrderSignoff pendingSignoff = VmsWorkOrderSignoff.builder()
                .id(100L)
                .workOrderId(10L)
                .signoffType(SignoffType.RESIDENT_OTP)
                .otpCode("123456")
                .isVerified(false)
                .build();

        when(workOrderRepo.findByIdAndCommunityId(10L, 1L)).thenReturn(Optional.of(wo));
        when(signoffRepo.findTopByWorkOrderIdAndSignoffTypeOrderByGeneratedAtDesc(10L, SignoffType.RESIDENT_OTP))
                .thenReturn(Optional.of(pendingSignoff));
        when(workOrderRepo.save(any(VmsWorkOrder.class))).thenAnswer(i -> i.getArgument(0));

        HandoverSignoffRequestDto dto = HandoverSignoffRequestDto.builder()
                .signoffType(SignoffType.RESIDENT_OTP)
                .otpCode("123456")
                .rating(5)
                .feedbackNotes("Clean plumbing repair completed promptly")
                .actualCost(BigDecimal.valueOf(1200.00))
                .completionNotes("Replaced ball valve and washer")
                .build();

        WorkOrderResponse completed = workOrderService.completeWithSignoff(10L, 1L, dto);

        assertNotNull(completed);
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals(BigDecimal.valueOf(1200.00), completed.getActualCost());
        assertTrue(pendingSignoff.getIsVerified());
        verify(slaService).evaluateWorkOrderSla(any(VmsWorkOrder.class));
    }

    @Test
    void testCompleteWithSignoff_InvalidOtp_ThrowsException() {
        VmsWorkOrder wo = VmsWorkOrder.builder().id(10L).build();
        VmsWorkOrderSignoff pendingSignoff = VmsWorkOrderSignoff.builder()
                .id(100L)
                .otpCode("123456")
                .build();

        when(workOrderRepo.findByIdAndCommunityId(10L, 1L)).thenReturn(Optional.of(wo));
        when(signoffRepo.findTopByWorkOrderIdAndSignoffTypeOrderByGeneratedAtDesc(10L, SignoffType.RESIDENT_OTP))
                .thenReturn(Optional.of(pendingSignoff));

        HandoverSignoffRequestDto dto = HandoverSignoffRequestDto.builder()
                .signoffType(SignoffType.RESIDENT_OTP)
                .otpCode("999999") // Wrong OTP
                .build();

        assertThrows(IllegalArgumentException.class, () -> workOrderService.completeWithSignoff(10L, 1L, dto));
    }
}
