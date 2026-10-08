package com.manacommunity.api.vendor.service;

import com.manacommunity.api.vendor.dto.CreateContractSlaRequestDto;
import com.manacommunity.api.vendor.dto.ReviewBreachRequestDto;
import com.manacommunity.api.vendor.dto.SlaBreachResponseDto;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.entity.VmsSlaBreachLog.BreachStatus;
import com.manacommunity.api.vendor.repository.VmsContractRepository;
import com.manacommunity.api.vendor.repository.VmsContractSlaRepository;
import com.manacommunity.api.vendor.repository.VmsSlaBreachLogRepository;
import com.manacommunity.common.model.Community;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VmsSlaMonitoringServiceTest {

    @Mock
    private VmsContractSlaRepository contractSlaRepository;

    @Mock
    private VmsSlaBreachLogRepository breachLogRepository;

    @Mock
    private VmsContractRepository contractRepository;

    @InjectMocks
    private VmsSlaMonitoringService slaService;

    @Test
    void testCreateContractSla_Success() {
        VmsContract mockContract = VmsContract.builder().id(10L).build();
        when(contractRepository.findById(10L)).thenReturn(Optional.of(mockContract));
        when(contractSlaRepository.save(any(VmsContractSla.class))).thenAnswer(i -> {
            VmsContractSla s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        CreateContractSlaRequestDto dto = CreateContractSlaRequestDto.builder()
                .metricName("Plumbing Turnaround Time")
                .targetValue("24")
                .unit("Hours")
                .penaltyAmount(BigDecimal.valueOf(500.00))
                .build();

        VmsContractSla created = slaService.createContractSla(10L, dto);

        assertNotNull(created);
        assertEquals("Plumbing Turnaround Time", created.getMetricName());
        assertEquals("24", created.getTargetValue());
        assertEquals(BigDecimal.valueOf(500.00), created.getPenaltyAmount());
    }

    @Test
    void testEvaluateWorkOrderSla_BreachLoggedWhenExceeded() {
        Community community = Community.builder().id(1L).build();
        VmsVendor vendor = VmsVendor.builder().id(5L).build();
        VmsContract contract = VmsContract.builder().id(10L).status(VmsContract.ContractStatus.ACTIVE).build();

        VmsContractSla sla = VmsContractSla.builder()
                .id(100L)
                .contract(contract)
                .metricName("Emergency Repair Response")
                .targetValue("4")
                .unit("Hours")
                .penaltyAmount(BigDecimal.valueOf(1000.00))
                .build();

        VmsWorkOrder workOrder = VmsWorkOrder.builder()
                .id(50L)
                .community(community)
                .vendor(vendor)
                .scheduledDate(LocalDate.now().minusDays(1)) // 24 hours ago
                .completedDate(LocalDateTime.now())
                .build();

        when(contractRepository.findByVendorId(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contract)));
        when(contractSlaRepository.findByContractId(10L)).thenReturn(List.of(sla));
        when(breachLogRepository.save(any(VmsSlaBreachLog.class))).thenAnswer(i -> {
            VmsSlaBreachLog b = i.getArgument(0);
            b.setId(1L);
            return b;
        });

        List<VmsSlaBreachLog> breaches = slaService.evaluateWorkOrderSla(workOrder);

        assertNotNull(breaches);
        assertEquals(1, breaches.size());
        assertEquals(BigDecimal.valueOf(1000.00), breaches.get(0).getPenaltyAmount());
        assertEquals(BreachStatus.PENDING_REVIEW, breaches.get(0).getStatus());
    }

    @Test
    void testReviewBreach_AppliesPenalty() {
        VmsSlaBreachLog breach = VmsSlaBreachLog.builder()
                .id(1L)
                .communityId(1L)
                .status(BreachStatus.PENDING_REVIEW)
                .penaltyAmount(BigDecimal.valueOf(500.00))
                .build();

        when(breachLogRepository.findByIdAndCommunityId(1L, 1L)).thenReturn(Optional.of(breach));
        when(breachLogRepository.save(any(VmsSlaBreachLog.class))).thenAnswer(i -> i.getArgument(0));

        ReviewBreachRequestDto dto = ReviewBreachRequestDto.builder()
                .status(BreachStatus.PENALTY_APPLIED)
                .remarks("Vendor failed to communicate delay")
                .reviewedBy(99L)
                .build();

        SlaBreachResponseDto response = slaService.reviewBreach(1L, 1L, dto);

        assertNotNull(response);
        assertEquals(BreachStatus.PENALTY_APPLIED, response.getStatus());
        assertEquals("Vendor failed to communicate delay", response.getRemarks());
    }
}
