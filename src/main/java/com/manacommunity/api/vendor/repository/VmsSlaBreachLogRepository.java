package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsSlaBreachLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VmsSlaBreachLogRepository extends JpaRepository<VmsSlaBreachLog, Long> {
    Page<VmsSlaBreachLog> findByCommunityIdOrderByBreachTimeDesc(Long communityId, Pageable pageable);
    Page<VmsSlaBreachLog> findByVendorIdOrderByBreachTimeDesc(Long vendorId, Pageable pageable);
    List<VmsSlaBreachLog> findByWorkOrderId(Long workOrderId);
    Optional<VmsSlaBreachLog> findByIdAndCommunityId(Long id, Long communityId);
}
