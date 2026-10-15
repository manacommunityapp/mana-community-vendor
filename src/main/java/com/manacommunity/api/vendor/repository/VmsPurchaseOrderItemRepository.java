package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsPurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmsPurchaseOrderItemRepository extends JpaRepository<VmsPurchaseOrderItem, Long> {
    List<VmsPurchaseOrderItem> findByPurchaseOrderId(Long poId);
}
