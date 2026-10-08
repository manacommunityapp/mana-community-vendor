package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsContractSla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmsContractSlaRepository extends JpaRepository<VmsContractSla, Long> {
    List<VmsContractSla> findByContractIdOrderBySortOrderAsc(Long contractId);
    List<VmsContractSla> findByContractId(Long contractId);
}
