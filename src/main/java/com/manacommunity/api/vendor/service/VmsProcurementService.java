package com.manacommunity.api.vendor.service;

import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.api.vendor.dto.*;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VmsProcurementService {

    private final VmsPurchaseRequestRepository prRepo;
    private final VmsPurchaseRequestItemRepository prItemRepo;
    private final VmsQuotationRepository quotationRepo;
    private final VmsQuotationItemRepository quotationItemRepo;
    private final VmsPurchaseOrderRepository poRepo;
    private final VmsPurchaseOrderItemRepository poItemRepo;
    private final VmsGoodsReceiptRepository grnRepo;
    private final VmsGoodsReceiptItemRepository grnItemRepo;
    private final VmsInvoiceRepository invoiceRepo;
    private final VmsInvoiceItemRepository invoiceItemRepo;
    private final VmsVendorRepository vendorRepo;

    // ─── Purchase Requests ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PurchaseRequestDto> getPurchaseRequests(Long communityId, String status, Pageable pageable) {
        Page<VmsPurchaseRequest> page;
        if (status != null && !status.isBlank()) {
            try {
                VmsPurchaseRequest.PurchaseRequestStatus st = VmsPurchaseRequest.PurchaseRequestStatus.valueOf(status.toUpperCase());
                page = prRepo.findByCommunityIdAndStatus(communityId, st, pageable);
            } catch (Exception e) {
                page = prRepo.findByCommunityId(communityId, pageable);
            }
        } else {
            page = prRepo.findByCommunityId(communityId, pageable);
        }
        return page.map(this::mapToPrDto);
    }

    @Transactional
    public PurchaseRequestDto createPurchaseRequest(PurchaseRequestDto dto, AppUser creator, Community community) {
        String reqNumber = "PR-" + System.currentTimeMillis() % 1000000;

        VmsPurchaseRequest pr = VmsPurchaseRequest.builder()
                .requestNumber(reqNumber)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(VmsPurchaseRequest.PurchaseRequestStatus.DRAFT)
                .priority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM")
                .requiredByDate(dto.getRequiredByDate())
                .department(dto.getDepartment())
                .totalEstimatedCost(dto.getTotalEstimatedCost() != null ? dto.getTotalEstimatedCost() : BigDecimal.ZERO)
                .createdByUser(creator)
                .community(community)
                .build();

        VmsPurchaseRequest saved = prRepo.save(pr);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PurchaseRequestDto.PurchaseRequestItemDto it : dto.getItems()) {
                VmsPurchaseRequestItem item = VmsPurchaseRequestItem.builder()
                        .request(saved)
                        .itemName(it.getItemName())
                        .specification(it.getSpecification())
                        .quantity(it.getQuantity() != null ? it.getQuantity() : BigDecimal.ONE)
                        .unit(it.getUnit())
                        .estimatedPrice(it.getEstimatedPrice() != null ? it.getEstimatedPrice() : BigDecimal.ZERO)
                        .sortOrder(it.getSortOrder() != null ? it.getSortOrder() : 0)
                        .build();
                prItemRepo.save(item);
            }
        }

        return mapToPrDto(saved);
    }

    @Transactional
    public PurchaseRequestDto approvePurchaseRequest(Long id, Long communityId, AppUser approver) {
        VmsPurchaseRequest pr = prRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", id));

        // Maker-Checker Separation: Creator cannot approve their own purchase request
        if (pr.getCreatedByUser() != null && pr.getCreatedByUser().getId().equals(approver.getId())) {
            throw new IllegalStateException("Maker-checker violation: Requester cannot approve their own purchase request.");
        }

        pr.setStatus(VmsPurchaseRequest.PurchaseRequestStatus.APPROVED);
        pr.setApprovedByUser(approver);
        pr.setApprovedAt(LocalDateTime.now());
        return mapToPrDto(prRepo.save(pr));
    }

    @Transactional
    public PurchaseRequestDto rejectPurchaseRequest(Long id, Long communityId, String reason, AppUser approver) {
        VmsPurchaseRequest pr = prRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", id));
        pr.setStatus(VmsPurchaseRequest.PurchaseRequestStatus.REJECTED);
        pr.setDescription((pr.getDescription() != null ? pr.getDescription() + "\n" : "") + "Rejection reason: " + reason);
        return mapToPrDto(prRepo.save(pr));
    }

    // ─── Quotations ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<QuotationDto> getQuotations(Long communityId, Long purchaseRequestId, Pageable pageable) {
        Page<VmsQuotation> page;
        if (purchaseRequestId != null) {
            page = quotationRepo.findByCommunityIdAndPurchaseRequestId(communityId, purchaseRequestId, pageable);
        } else {
            page = quotationRepo.findByCommunityId(communityId, pageable);
        }
        return page.map(this::mapToQuotationDto);
    }

    @Transactional
    public QuotationDto submitQuotation(QuotationDto dto, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(dto.getVendorId(), communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", dto.getVendorId()));

        VmsPurchaseRequest pr = null;
        if (dto.getPurchaseRequestId() != null) {
            pr = prRepo.findByIdAndCommunityId(dto.getPurchaseRequestId(), communityId).orElse(null);
        }

        String qNumber = "QT-" + System.currentTimeMillis() % 1000000;

        VmsQuotation quotation = VmsQuotation.builder()
                .quotationNumber(qNumber)
                .purchaseRequest(pr)
                .vendor(vendor)
                .status(VmsQuotation.QuotationStatus.SUBMITTED)
                .totalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO)
                .taxAmount(dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO)
                .discountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO)
                .grandTotal(dto.getGrandTotal() != null ? dto.getGrandTotal() : BigDecimal.ZERO)
                .deliveryDays(dto.getDeliveryDays())
                .warrantyDays(dto.getWarrantyDays())
                .paymentTerms(dto.getPaymentTerms())
                .validityDays(dto.getValidityDays() != null ? dto.getValidityDays() : 30)
                .notes(dto.getNotes())
                .community(vendor.getCommunity())
                .submittedAt(LocalDateTime.now())
                .build();

        VmsQuotation saved = quotationRepo.save(quotation);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (QuotationDto.QuotationItemDto qi : dto.getItems()) {
                VmsQuotationItem item = VmsQuotationItem.builder()
                        .quotation(saved)
                        .itemName(qi.getItemName())
                        .specification(qi.getSpecification())
                        .quantity(qi.getQuantity() != null ? qi.getQuantity() : BigDecimal.ONE)
                        .unit(qi.getUnit())
                        .unitPrice(qi.getUnitPrice() != null ? qi.getUnitPrice() : BigDecimal.ZERO)
                        .taxPercent(qi.getTaxPercent() != null ? qi.getTaxPercent() : BigDecimal.ZERO)
                        .discountPercent(qi.getDiscountPercent() != null ? qi.getDiscountPercent() : BigDecimal.ZERO)
                        .total(qi.getTotal() != null ? qi.getTotal() : BigDecimal.ZERO)
                        .sortOrder(qi.getSortOrder() != null ? qi.getSortOrder() : 0)
                        .build();
                quotationItemRepo.save(item);
            }
        }

        return mapToQuotationDto(saved);
    }

    // ─── Purchase Orders ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> getPurchaseOrders(Long communityId, String status, Pageable pageable) {
        Page<VmsPurchaseOrder> page;
        if (status != null && !status.isBlank()) {
            try {
                VmsPurchaseOrder.PurchaseOrderStatus st = VmsPurchaseOrder.PurchaseOrderStatus.valueOf(status.toUpperCase());
                page = poRepo.findByCommunityIdAndStatus(communityId, st, pageable);
            } catch (Exception e) {
                page = poRepo.findByCommunityId(communityId, pageable);
            }
        } else {
            page = poRepo.findByCommunityId(communityId, pageable);
        }
        return page.map(this::mapToPoDto);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Long id, Long communityId) {
        VmsPurchaseOrder po = poRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));
        return mapToPoDto(po);
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderDto dto, AppUser creator, Community community) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(dto.getVendorId(), community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", dto.getVendorId()));

        VmsQuotation quotation = null;
        if (dto.getQuotationId() != null) {
            quotation = quotationRepo.findById(dto.getQuotationId()).orElse(null);
        }

        String poNumber = "PO-" + System.currentTimeMillis() % 1000000;

        VmsPurchaseOrder po = VmsPurchaseOrder.builder()
                .poNumber(poNumber)
                .quotation(quotation)
                .vendor(vendor)
                .status(VmsPurchaseOrder.PurchaseOrderStatus.DRAFT)
                .totalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO)
                .taxAmount(dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO)
                .discountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO)
                .grandTotal(dto.getGrandTotal() != null ? dto.getGrandTotal() : BigDecimal.ZERO)
                .deliveryDate(dto.getDeliveryDate())
                .deliveryAddress(dto.getDeliveryAddress())
                .paymentTerms(dto.getPaymentTerms())
                .termsConditions(dto.getTermsConditions())
                .createdByUser(creator)
                .community(community)
                .build();

        VmsPurchaseOrder saved = poRepo.save(po);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (PurchaseOrderDto.PurchaseOrderItemDto poi : dto.getItems()) {
                VmsPurchaseOrderItem item = VmsPurchaseOrderItem.builder()
                        .purchaseOrder(saved)
                        .itemName(poi.getItemName())
                        .specification(poi.getSpecification())
                        .quantity(poi.getQuantity() != null ? poi.getQuantity() : BigDecimal.ONE)
                        .unit(poi.getUnit())
                        .unitPrice(poi.getUnitPrice() != null ? poi.getUnitPrice() : BigDecimal.ZERO)
                        .taxPercent(poi.getTaxPercent() != null ? poi.getTaxPercent() : BigDecimal.ZERO)
                        .discountPercent(poi.getDiscountPercent() != null ? poi.getDiscountPercent() : BigDecimal.ZERO)
                        .total(poi.getTotal() != null ? poi.getTotal() : BigDecimal.ZERO)
                        .receivedQuantity(BigDecimal.ZERO)
                        .sortOrder(poi.getSortOrder() != null ? poi.getSortOrder() : 0)
                        .build();
                poItemRepo.save(item);
            }
        }

        return mapToPoDto(saved);
    }

    @Transactional
    public PurchaseOrderDto updatePurchaseOrderStatus(Long id, String status, Long communityId, AppUser user) {
        VmsPurchaseOrder po = poRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        VmsPurchaseOrder.PurchaseOrderStatus newStatus = VmsPurchaseOrder.PurchaseOrderStatus.valueOf(status.toUpperCase());

        // Maker-Checker validation on approval
        if (newStatus == VmsPurchaseOrder.PurchaseOrderStatus.APPROVED) {
            if (po.getCreatedByUser() != null && po.getCreatedByUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Maker-checker violation: PO creator cannot approve their own Purchase Order.");
            }
            po.setApprovedByUser(user);
            po.setApprovedAt(LocalDateTime.now());
        }

        po.setStatus(newStatus);
        return mapToPoDto(poRepo.save(po));
    }

    // ─── Goods Receipts (GRN) ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GoodsReceiptDto> getGoodsReceipts(Long communityId, Long purchaseOrderId, Pageable pageable) {
        Page<VmsGoodsReceipt> page;
        if (purchaseOrderId != null) {
            page = grnRepo.findByCommunityIdAndPurchaseOrderId(communityId, purchaseOrderId, pageable);
        } else {
            page = grnRepo.findByCommunityId(communityId, pageable);
        }
        return page.map(this::mapToGrnDto);
    }

    @Transactional
    public GoodsReceiptDto createGoodsReceipt(GoodsReceiptDto dto, AppUser receiver, Community community) {
        VmsPurchaseOrder po = poRepo.findByIdAndCommunityId(dto.getPurchaseOrderId(), community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", dto.getPurchaseOrderId()));

        String grnNumber = "GRN-" + System.currentTimeMillis() % 1000000;

        VmsGoodsReceipt grn = VmsGoodsReceipt.builder()
                .grnNumber(grnNumber)
                .purchaseOrder(po)
                .vendor(po.getVendor())
                .status("ACCEPTED")
                .receivedDate(dto.getReceivedDate() != null ? dto.getReceivedDate() : LocalDate.now())
                .receivedByUser(receiver)
                .notes(dto.getNotes())
                .community(community)
                .build();

        VmsGoodsReceipt savedGrn = grnRepo.save(grn);

        List<VmsPurchaseOrderItem> poItems = poItemRepo.findByPurchaseOrderId(po.getId());
        Map<Long, VmsPurchaseOrderItem> poItemMap = poItems.stream().collect(Collectors.toMap(VmsPurchaseOrderItem::getId, it -> it));

        boolean allFulfilled = true;

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (GoodsReceiptDto.GoodsReceiptItemDto gri : dto.getItems()) {
                VmsPurchaseOrderItem poItem = gri.getPoItemId() != null ? poItemMap.get(gri.getPoItemId()) : null;

                BigDecimal accepted = gri.getAcceptedQuantity() != null ? gri.getAcceptedQuantity() : BigDecimal.ZERO;
                BigDecimal rejected = gri.getRejectedQuantity() != null ? gri.getRejectedQuantity() : BigDecimal.ZERO;
                BigDecimal received = gri.getReceivedQuantity() != null ? gri.getReceivedQuantity() : accepted.add(rejected);

                VmsGoodsReceiptItem grnItem = VmsGoodsReceiptItem.builder()
                        .goodsReceipt(savedGrn)
                        .poItem(poItem)
                        .itemName(gri.getItemName() != null ? gri.getItemName() : (poItem != null ? poItem.getItemName() : "Item"))
                        .orderedQuantity(gri.getOrderedQuantity() != null ? gri.getOrderedQuantity() : (poItem != null ? poItem.getQuantity() : received))
                        .receivedQuantity(received)
                        .acceptedQuantity(accepted)
                        .rejectedQuantity(rejected)
                        .rejectionReason(gri.getRejectionReason())
                        .sortOrder(gri.getSortOrder() != null ? gri.getSortOrder() : 0)
                        .build();
                grnItemRepo.save(grnItem);

                // Update PO Item cumulative received quantity
                if (poItem != null) {
                    BigDecimal currentRec = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : BigDecimal.ZERO;
                    BigDecimal newRec = currentRec.add(accepted);
                    poItem.setReceivedQuantity(newRec);
                    poItemRepo.save(poItem);

                    if (newRec.compareTo(poItem.getQuantity()) < 0) {
                        allFulfilled = false;
                    }
                }
            }
        }

        // Update PO status
        po.setStatus(allFulfilled ? VmsPurchaseOrder.PurchaseOrderStatus.RECEIVED : VmsPurchaseOrder.PurchaseOrderStatus.PARTIALLY_RECEIVED);
        poRepo.save(po);

        log.info("Created Goods Receipt: grn={}, po={}, allFulfilled={}", grnNumber, po.getPoNumber(), allFulfilled);
        return mapToGrnDto(savedGrn);
    }

    // ─── Three-Way Matching Engine ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public ThreeWayMatchResultDto matchThreeWay(Long invoiceId, Long communityId) {
        VmsInvoice invoice = invoiceRepo.findByIdAndCommunityId(invoiceId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        if (invoice.getPurchaseOrder() == null) {
            return ThreeWayMatchResultDto.builder()
                    .invoiceId(invoice.getId())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .matchStatus("NO_LINKED_PO")
                    .summaryMessage("This invoice is not linked to a Purchase Order.")
                    .build();
        }

        VmsPurchaseOrder po = invoice.getPurchaseOrder();
        List<VmsGoodsReceipt> grns = grnRepo.findByPurchaseOrderId(po.getId());
        List<VmsPurchaseOrderItem> poItems = poItemRepo.findByPurchaseOrderId(po.getId());
        List<VmsInvoiceItem> invoiceItems = invoiceItemRepo.findByInvoiceId(invoice.getId());

        // Aggregate accepted quantities from all GRNs for this PO
        Map<String, BigDecimal> grnAcceptedByItem = new HashMap<>();
        for (VmsGoodsReceipt grn : grns) {
            List<VmsGoodsReceiptItem> items = grnItemRepo.findByGoodsReceiptId(grn.getId());
            for (VmsGoodsReceiptItem item : items) {
                String key = item.getItemName().trim().toLowerCase();
                grnAcceptedByItem.merge(key, item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : BigDecimal.ZERO, BigDecimal::add);
            }
        }

        // Compare each billed item against PO and GRN
        List<ThreeWayMatchResultDto.ItemMatchDetail> itemDetails = new ArrayList<>();
        boolean isQtyMatched = true;
        boolean isPriceMatched = true;

        BigDecimal poTotal = po.getGrandTotal() != null ? po.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal invoiceTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal grnAcceptedTotal = BigDecimal.ZERO;

        for (VmsInvoiceItem invItem : invoiceItems) {
            String key = invItem.getDescription().trim().toLowerCase();
            VmsPurchaseOrderItem poItem = poItems.stream()
                    .filter(p -> p.getItemName().trim().equalsIgnoreCase(invItem.getDescription().trim()))
                    .findFirst()
                    .orElse(null);

            BigDecimal billedQty = invItem.getQuantity() != null ? invItem.getQuantity() : BigDecimal.ZERO;
            BigDecimal billedPrice = invItem.getUnitPrice() != null ? invItem.getUnitPrice() : BigDecimal.ZERO;

            BigDecimal poQty = poItem != null ? poItem.getQuantity() : BigDecimal.ZERO;
            BigDecimal poPrice = poItem != null ? poItem.getUnitPrice() : BigDecimal.ZERO;

            BigDecimal grnAccepted = grnAcceptedByItem.getOrDefault(key, BigDecimal.ZERO);
            grnAcceptedTotal = grnAcceptedTotal.add(grnAccepted.multiply(poPrice));

            // Quantity Check: Billed quantity must not exceed accepted quantity from GRN
            boolean itemQtyMatch = billedQty.compareTo(grnAccepted) <= 0;
            if (!itemQtyMatch) {
                isQtyMatched = false;
            }

            // Price Check: Price per unit must be within ±2% tolerance of PO price
            boolean itemPriceMatch = true;
            if (poPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal variance = billedPrice.subtract(poPrice).abs();
                BigDecimal pct = variance.divide(poPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                if (pct.compareTo(BigDecimal.valueOf(2.0)) > 0) {
                    itemPriceMatch = false;
                    isPriceMatched = false;
                }
            }

            String note = "OK";
            if (!itemQtyMatch) {
                note = "Billed (" + billedQty + ") exceeds GRN accepted (" + grnAccepted + ")";
            } else if (!itemPriceMatch) {
                note = "Price variance exceeds 2% (PO: " + poPrice + ", Billed: " + billedPrice + ")";
            }

            itemDetails.add(ThreeWayMatchResultDto.ItemMatchDetail.builder()
                    .itemName(invItem.getDescription())
                    .orderedQty(poQty)
                    .receivedQty(grnAccepted)
                    .acceptedQty(grnAccepted)
                    .billedQty(billedQty)
                    .unitPricePo(poPrice)
                    .unitPriceBilled(billedPrice)
                    .isQtyMatched(itemQtyMatch)
                    .isPriceMatched(itemPriceMatch)
                    .note(note)
                    .build());
        }

        // Price variance on total
        BigDecimal priceVariance = invoiceTotal.subtract(poTotal);
        BigDecimal priceVariancePct = poTotal.compareTo(BigDecimal.ZERO) > 0
                ? priceVariance.abs().divide(poTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        boolean isPriceWithinTol = priceVariancePct.compareTo(BigDecimal.valueOf(2.0)) <= 0;

        String matchStatus;
        String summary;
        if (isQtyMatched && isPriceWithinTol) {
            matchStatus = "MATCHED";
            summary = "Three-way match SUCCESSFUL. Quantities verified against GRN, prices match PO within tolerance.";
        } else if (!isQtyMatched) {
            matchStatus = "QUANTITY_MISMATCH";
            summary = "Three-way match FAILED: Billed quantity exceeds accepted goods on delivery receipt.";
        } else {
            matchStatus = "PRICE_MISMATCH";
            summary = "Three-way match FAILED: Invoice total has a " + priceVariancePct + "% variance from Purchase Order.";
        }

        return ThreeWayMatchResultDto.builder()
                .purchaseOrderId(po.getId())
                .poNumber(po.getPoNumber())
                .goodsReceiptId(!grns.isEmpty() ? grns.get(0).getId() : null)
                .grnNumber(!grns.isEmpty() ? grns.get(0).getGrnNumber() : null)
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .matchStatus(matchStatus)
                .poTotal(poTotal)
                .grnAcceptedTotal(grnAcceptedTotal)
                .invoiceTotal(invoiceTotal)
                .priceVariance(priceVariance)
                .priceVariancePercentage(priceVariancePct)
                .isPriceWithinTolerance(isPriceWithinTol)
                .isQuantityMatched(isQtyMatched)
                .itemDetails(itemDetails)
                .summaryMessage(summary)
                .build();
    }

    // ─── DTO Mappings ───────────────────────────────────────────────────────

    private PurchaseRequestDto mapToPrDto(VmsPurchaseRequest pr) {
        List<VmsPurchaseRequestItem> items = prItemRepo.findByRequestId(pr.getId());
        return PurchaseRequestDto.builder()
                .id(pr.getId())
                .requestNumber(pr.getRequestNumber())
                .title(pr.getTitle())
                .description(pr.getDescription())
                .status(pr.getStatus() != null ? pr.getStatus().name() : "DRAFT")
                .priority(pr.getPriority())
                .requiredByDate(pr.getRequiredByDate())
                .department(pr.getDepartment())
                .totalEstimatedCost(pr.getTotalEstimatedCost())
                .approvedByName(pr.getApprovedByUser() != null ? pr.getApprovedByUser().getFullName() : null)
                .approvedAt(pr.getApprovedAt())
                .communityId(pr.getCommunity() != null ? pr.getCommunity().getId() : null)
                .createdByName(pr.getCreatedByUser() != null ? pr.getCreatedByUser().getFullName() : null)
                .createdAt(pr.getCreatedAt())
                .items(items.stream().map(it -> PurchaseRequestDto.PurchaseRequestItemDto.builder()
                        .id(it.getId())
                        .itemName(it.getItemName())
                        .specification(it.getSpecification())
                        .quantity(it.getQuantity())
                        .unit(it.getUnit())
                        .estimatedPrice(it.getEstimatedPrice())
                        .sortOrder(it.getSortOrder())
                        .build()).toList())
                .build();
    }

    private QuotationDto mapToQuotationDto(VmsQuotation q) {
        List<VmsQuotationItem> items = quotationItemRepo.findByQuotationId(q.getId());
        return QuotationDto.builder()
                .id(q.getId())
                .quotationNumber(q.getQuotationNumber())
                .purchaseRequestId(q.getPurchaseRequest() != null ? q.getPurchaseRequest().getId() : null)
                .vendorId(q.getVendor() != null ? q.getVendor().getId() : null)
                .vendorName(q.getVendor() != null ? q.getVendor().getBusinessName() : null)
                .status(q.getStatus() != null ? q.getStatus().name() : "SUBMITTED")
                .totalAmount(q.getTotalAmount())
                .taxAmount(q.getTaxAmount())
                .discountAmount(q.getDiscountAmount())
                .grandTotal(q.getGrandTotal())
                .deliveryDays(q.getDeliveryDays())
                .warrantyDays(q.getWarrantyDays())
                .paymentTerms(q.getPaymentTerms())
                .validityDays(q.getValidityDays())
                .notes(q.getNotes())
                .communityId(q.getCommunity() != null ? q.getCommunity().getId() : null)
                .submittedAt(q.getSubmittedAt())
                .createdAt(q.getCreatedAt())
                .items(items.stream().map(it -> QuotationDto.QuotationItemDto.builder()
                        .id(it.getId())
                        .itemName(it.getItemName())
                        .specification(it.getSpecification())
                        .quantity(it.getQuantity())
                        .unit(it.getUnit())
                        .unitPrice(it.getUnitPrice())
                        .taxPercent(it.getTaxPercent())
                        .discountPercent(it.getDiscountPercent())
                        .total(it.getTotal())
                        .sortOrder(it.getSortOrder())
                        .build()).toList())
                .build();
    }

    private PurchaseOrderDto mapToPoDto(VmsPurchaseOrder po) {
        List<VmsPurchaseOrderItem> items = poItemRepo.findByPurchaseOrderId(po.getId());
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .vendorId(po.getVendor() != null ? po.getVendor().getId() : null)
                .vendorName(po.getVendor() != null ? po.getVendor().getBusinessName() : null)
                .quotationId(po.getQuotation() != null ? po.getQuotation().getId() : null)
                .status(po.getStatus() != null ? po.getStatus().name() : "DRAFT")
                .totalAmount(po.getTotalAmount())
                .taxAmount(po.getTaxAmount())
                .discountAmount(po.getDiscountAmount())
                .grandTotal(po.getGrandTotal())
                .deliveryDate(po.getDeliveryDate())
                .deliveryAddress(po.getDeliveryAddress())
                .paymentTerms(po.getPaymentTerms())
                .termsConditions(po.getTermsConditions())
                .approvedByName(po.getApprovedByUser() != null ? po.getApprovedByUser().getFullName() : null)
                .approvedAt(po.getApprovedAt())
                .communityId(po.getCommunity() != null ? po.getCommunity().getId() : null)
                .createdByName(po.getCreatedByUser() != null ? po.getCreatedByUser().getFullName() : null)
                .createdAt(po.getCreatedAt())
                .items(items.stream().map(it -> PurchaseOrderDto.PurchaseOrderItemDto.builder()
                        .id(it.getId())
                        .itemName(it.getItemName())
                        .specification(it.getSpecification())
                        .quantity(it.getQuantity())
                        .unit(it.getUnit())
                        .unitPrice(it.getUnitPrice())
                        .taxPercent(it.getTaxPercent())
                        .discountPercent(it.getDiscountPercent())
                        .total(it.getTotal())
                        .receivedQuantity(it.getReceivedQuantity())
                        .sortOrder(it.getSortOrder())
                        .build()).toList())
                .build();
    }

    private GoodsReceiptDto mapToGrnDto(VmsGoodsReceipt grn) {
        List<VmsGoodsReceiptItem> items = grnItemRepo.findByGoodsReceiptId(grn.getId());
        return GoodsReceiptDto.builder()
                .id(grn.getId())
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getId() : null)
                .poNumber(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getPoNumber() : null)
                .vendorId(grn.getVendor() != null ? grn.getVendor().getId() : null)
                .vendorName(grn.getVendor() != null ? grn.getVendor().getBusinessName() : null)
                .status(grn.getStatus())
                .receivedDate(grn.getReceivedDate())
                .receivedByUserId(grn.getReceivedByUser() != null ? grn.getReceivedByUser().getId() : null)
                .receivedByUserName(grn.getReceivedByUser() != null ? grn.getReceivedByUser().getFullName() : null)
                .notes(grn.getNotes())
                .communityId(grn.getCommunity() != null ? grn.getCommunity().getId() : null)
                .createdAt(grn.getCreatedAt())
                .items(items.stream().map(it -> GoodsReceiptDto.GoodsReceiptItemDto.builder()
                        .id(it.getId())
                        .poItemId(it.getPoItem() != null ? it.getPoItem().getId() : null)
                        .itemName(it.getItemName())
                        .orderedQuantity(it.getOrderedQuantity())
                        .receivedQuantity(it.getReceivedQuantity())
                        .acceptedQuantity(it.getAcceptedQuantity())
                        .rejectedQuantity(it.getRejectedQuantity())
                        .rejectionReason(it.getRejectionReason())
                        .sortOrder(it.getSortOrder())
                        .build()).toList())
                .build();
    }
}
