package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsGoodsReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VmsGoodsReceiptRepository extends JpaRepository<VmsGoodsReceipt, Long> {
    Page<VmsGoodsReceipt> findByCommunityId(Long communityId, Pageable pageable);
    Page<VmsGoodsReceipt> findByCommunityIdAndPurchaseOrderId(Long communityId, Long purchaseOrderId, Pageable pageable);
    List<VmsGoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId);
    Optional<VmsGoodsReceipt> findByIdAndCommunityId(Long id, Long communityId);
    Optional<VmsGoodsReceipt> findByGrnNumber(String grnNumber);
}
