package com.manacommunity.api.vendor.controller;

import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.*;
import com.manacommunity.api.vendor.service.VmsProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vendor/procurement")
@RequiredArgsConstructor
public class VmsProcurementController {

    private final VmsProcurementService procurementService;
    private final LoggedInUserService loggedInUserService;

    // ─── Purchase Requests ──────────────────────────────────────────────────

    @GetMapping("/requests")
    public ResponseEntity<Page<PurchaseRequestDto>> getRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(procurementService.getPurchaseRequests(user.getCommunity().getId(), status, pageable));
    }

    @PostMapping("/requests")
    public ResponseEntity<PurchaseRequestDto> createRequest(
            @RequestBody PurchaseRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procurementService.createPurchaseRequest(dto, user, user.getCommunity()));
    }

    @PutMapping("/requests/{id}/approve")
    @PreAuthorize("hasAuthority('Manage Vendors') or hasRole('ADMIN') or hasRole('TREASURER')")
    public ResponseEntity<PurchaseRequestDto> approveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(procurementService.approvePurchaseRequest(id, user.getCommunity().getId(), user));
    }

    @PutMapping("/requests/{id}/reject")
    @PreAuthorize("hasAuthority('Manage Vendors') or hasRole('ADMIN') or hasRole('TREASURER')")
    public ResponseEntity<PurchaseRequestDto> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = body != null && body.containsKey("reason") ? body.get("reason") : "Rejected by management";
        return ResponseEntity.ok(procurementService.rejectPurchaseRequest(id, user.getCommunity().getId(), reason, user));
    }

    // ─── Quotations ─────────────────────────────────────────────────────────

    @GetMapping("/quotations")
    public ResponseEntity<Page<QuotationDto>> getQuotations(
            @RequestParam(required = false) Long purchaseRequestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(procurementService.getQuotations(user.getCommunity().getId(), purchaseRequestId, pageable));
    }

    @PostMapping("/quotations")
    public ResponseEntity<QuotationDto> submitQuotation(
            @RequestBody QuotationDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procurementService.submitQuotation(dto, user.getCommunity().getId()));
    }

    // ─── Purchase Orders ────────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<Page<PurchaseOrderDto>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(procurementService.getPurchaseOrders(user.getCommunity().getId(), status, pageable));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<PurchaseOrderDto> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(procurementService.getPurchaseOrderById(id, user.getCommunity().getId()));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('Manage Vendors') or hasRole('ADMIN') or hasRole('TREASURER')")
    public ResponseEntity<PurchaseOrderDto> createOrder(
            @RequestBody PurchaseOrderDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procurementService.createPurchaseOrder(dto, user, user.getCommunity()));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<PurchaseOrderDto> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(procurementService.updatePurchaseOrderStatus(id, status, user.getCommunity().getId(), user));
    }

    // ─── Goods Receipts (GRN) ───────────────────────────────────────────────

    @GetMapping("/grn")
    public ResponseEntity<Page<GoodsReceiptDto>> getGoodsReceipts(
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(procurementService.getGoodsReceipts(user.getCommunity().getId(), purchaseOrderId, pageable));
    }

    @PostMapping("/grn")
    public ResponseEntity<GoodsReceiptDto> createGoodsReceipt(
            @RequestBody GoodsReceiptDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procurementService.createGoodsReceipt(dto, user, user.getCommunity()));
    }

    // ─── Three-Way Match ────────────────────────────────────────────────────

    @PostMapping("/invoices/{id}/match-3way")
    @PreAuthorize("hasAuthority('Manage Vendors') or hasRole('ADMIN') or hasRole('TREASURER')")
    public ResponseEntity<ThreeWayMatchResultDto> matchThreeWay(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(procurementService.matchThreeWay(id, user.getCommunity().getId()));
    }
}
