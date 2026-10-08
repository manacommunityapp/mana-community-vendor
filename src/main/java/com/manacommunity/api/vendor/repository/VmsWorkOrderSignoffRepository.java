package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsWorkOrderSignoff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VmsWorkOrderSignoffRepository extends JpaRepository<VmsWorkOrderSignoff, Long> {
    List<VmsWorkOrderSignoff> findByWorkOrderId(Long workOrderId);
    Optional<VmsWorkOrderSignoff> findTopByWorkOrderIdAndSignoffTypeOrderByGeneratedAtDesc(
            Long workOrderId, VmsWorkOrderSignoff.SignoffType signoffType);
}
