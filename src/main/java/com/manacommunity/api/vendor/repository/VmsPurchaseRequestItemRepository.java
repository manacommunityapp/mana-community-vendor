package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsPurchaseRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmsPurchaseRequestItemRepository extends JpaRepository<VmsPurchaseRequestItem, Long> {
    List<VmsPurchaseRequestItem> findByRequestId(Long requestId);
}
