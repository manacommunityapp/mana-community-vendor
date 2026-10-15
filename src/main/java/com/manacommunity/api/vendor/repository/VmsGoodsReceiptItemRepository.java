package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsGoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmsGoodsReceiptItemRepository extends JpaRepository<VmsGoodsReceiptItem, Long> {
    List<VmsGoodsReceiptItem> findByGoodsReceiptId(Long grnId);
    List<VmsGoodsReceiptItem> findByPoItemId(Long poItemId);
}
