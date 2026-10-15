package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsQuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmsQuotationItemRepository extends JpaRepository<VmsQuotationItem, Long> {
    List<VmsQuotationItem> findByQuotationId(Long quotationId);
}
