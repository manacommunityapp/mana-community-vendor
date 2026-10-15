package com.manacommunity.api.vendor;

import com.manacommunity.common.model.Community;
import com.manacommunity.api.vendor.dto.ThreeWayMatchResultDto;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import com.manacommunity.api.vendor.service.VmsProcurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreeWayMatchingTest {

    @Mock
    private VmsPurchaseOrderRepository poRepo;
    @Mock
    private VmsPurchaseOrderItemRepository poItemRepo;
    @Mock
    private VmsGoodsReceiptRepository grnRepo;
    @Mock
    private VmsGoodsReceiptItemRepository grnItemRepo;
    @Mock
    private VmsInvoiceRepository invoiceRepo;
    @Mock
    private VmsInvoiceItemRepository invoiceItemRepo;

    @InjectMocks
    private VmsProcurementService procurementService;

    private Community community;
    private VmsPurchaseOrder po;
    private VmsPurchaseOrderItem poItem;
    private VmsGoodsReceipt grn;
    private VmsGoodsReceiptItem grnItem;
    private VmsInvoice invoice;
    private VmsInvoiceItem invoiceItem;

    @BeforeEach
    void setUp() {
        community = new Community();
        community.setId(1L);

        po = VmsPurchaseOrder.builder()
                .id(100L)
                .poNumber("PO-2026-0001")
                .community(community)
                .grandTotal(BigDecimal.valueOf(1000.00))
                .status(VmsPurchaseOrder.PurchaseOrderStatus.APPROVED)
                .build();

        poItem = VmsPurchaseOrderItem.builder()
                .id(1L)
                .purchaseOrder(po)
                .itemName("Industrial Air Filters")
                .quantity(BigDecimal.valueOf(10))
                .unitPrice(BigDecimal.valueOf(100.00))
                .total(BigDecimal.valueOf(1000.00))
                .build();

        grn = VmsGoodsReceipt.builder()
                .id(200L)
                .grnNumber("GRN-2026-0001")
                .community(community)
                .purchaseOrder(po)
                .status("ACCEPTED")
                .build();

        grnItem = VmsGoodsReceiptItem.builder()
                .id(1L)
                .goodsReceipt(grn)
                .itemName("Industrial Air Filters")
                .receivedQuantity(BigDecimal.valueOf(10))
                .acceptedQuantity(BigDecimal.valueOf(10))
                .rejectedQuantity(BigDecimal.ZERO)
                .build();

        invoice = VmsInvoice.builder()
                .id(300L)
                .invoiceNumber("INV-2026-0001")
                .community(community)
                .purchaseOrder(po)
                .totalAmount(BigDecimal.valueOf(1000.00))
                .status(VmsInvoice.InvoiceStatus.SENT)
                .build();

        invoiceItem = VmsInvoiceItem.builder()
                .id(1L)
                .invoice(invoice)
                .description("Industrial Air Filters")
                .quantity(BigDecimal.valueOf(10))
                .unitPrice(BigDecimal.valueOf(100.00))
                .total(BigDecimal.valueOf(1000.00))
                .build();
    }

    @Test
    @DisplayName("Exact Match: PO, GRN accepted qty, and Invoice amounts match exactly")
    void testExactThreeWayMatch() {
        when(invoiceRepo.findByIdAndCommunityId(300L, 1L)).thenReturn(Optional.of(invoice));
        when(grnRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(grn));
        when(grnItemRepo.findByGoodsReceiptId(200L)).thenReturn(List.of(grnItem));
        when(poItemRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(poItem));
        when(invoiceItemRepo.findByInvoiceId(300L)).thenReturn(List.of(invoiceItem));

        ThreeWayMatchResultDto result = procurementService.matchThreeWay(300L, 1L);

        assertNotNull(result);
        assertEquals("MATCHED", result.getMatchStatus());
        assertTrue(result.getIsQuantityMatched());
        assertTrue(result.getIsPriceWithinTolerance());
        assertEquals(0, BigDecimal.valueOf(1000.00).compareTo(result.getInvoiceTotal()));
    }

    @Test
    @DisplayName("Quantity Discrepancy: Billed qty exceeds accepted GRN received quantity")
    void testQuantityMismatch() {
        // GRN only accepted 8 units (2 were damaged/shortfall)
        grnItem.setAcceptedQuantity(BigDecimal.valueOf(8));
        // Invoice billed for 10 units
        invoiceItem.setQuantity(BigDecimal.valueOf(10));

        when(invoiceRepo.findByIdAndCommunityId(300L, 1L)).thenReturn(Optional.of(invoice));
        when(grnRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(grn));
        when(grnItemRepo.findByGoodsReceiptId(200L)).thenReturn(List.of(grnItem));
        when(poItemRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(poItem));
        when(invoiceItemRepo.findByInvoiceId(300L)).thenReturn(List.of(invoiceItem));

        ThreeWayMatchResultDto result = procurementService.matchThreeWay(300L, 1L);

        assertNotNull(result);
        assertEquals("QUANTITY_MISMATCH", result.getMatchStatus());
        assertFalse(result.getIsQuantityMatched());
        assertFalse(result.getItemDetails().isEmpty());
        assertFalse(result.getItemDetails().get(0).getIsQtyMatched());
    }

    @Test
    @DisplayName("Price Variance Within 2% Tolerance: Should Pass Match")
    void testPriceVarianceWithinTolerance() {
        // PO price is 100.00, Invoice billed unit price is 101.50 (1.5% variance <= 2.0%)
        invoiceItem.setUnitPrice(BigDecimal.valueOf(101.50));
        invoice.setTotalAmount(BigDecimal.valueOf(1015.00));

        when(invoiceRepo.findByIdAndCommunityId(300L, 1L)).thenReturn(Optional.of(invoice));
        when(grnRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(grn));
        when(grnItemRepo.findByGoodsReceiptId(200L)).thenReturn(List.of(grnItem));
        when(poItemRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(poItem));
        when(invoiceItemRepo.findByInvoiceId(300L)).thenReturn(List.of(invoiceItem));

        ThreeWayMatchResultDto result = procurementService.matchThreeWay(300L, 1L);

        assertNotNull(result);
        assertEquals("MATCHED", result.getMatchStatus());
        assertTrue(result.getIsPriceWithinTolerance());
    }

    @Test
    @DisplayName("Price Variance Exceeds 2% Tolerance: Should Flag Price Mismatch")
    void testPriceVarianceExceedsTolerance() {
        // PO price is 100.00, Invoice billed unit price is 105.00 (5% variance > 2.0%)
        invoiceItem.setUnitPrice(BigDecimal.valueOf(105.00));
        invoice.setTotalAmount(BigDecimal.valueOf(1050.00));

        when(invoiceRepo.findByIdAndCommunityId(300L, 1L)).thenReturn(Optional.of(invoice));
        when(grnRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(grn));
        when(grnItemRepo.findByGoodsReceiptId(200L)).thenReturn(List.of(grnItem));
        when(poItemRepo.findByPurchaseOrderId(100L)).thenReturn(List.of(poItem));
        when(invoiceItemRepo.findByInvoiceId(300L)).thenReturn(List.of(invoiceItem));

        ThreeWayMatchResultDto result = procurementService.matchThreeWay(300L, 1L);

        assertNotNull(result);
        assertEquals("PRICE_MISMATCH", result.getMatchStatus());
        assertFalse(result.getIsPriceWithinTolerance());
    }
}
