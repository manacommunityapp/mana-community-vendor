package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmsInvoiceItemRepository extends JpaRepository<VmsInvoiceItem, Long> {
    List<VmsInvoiceItem> findByInvoiceId(Long invoiceId);
}
