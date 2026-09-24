package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.proposal.ProposalRequest;
import com.trade.dto.proposal.ProposalResponse;
import com.trade.service.LogisticsProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class LogisticsProposalController {

    private final LogisticsProposalService proposalService;

    /**
     * Logistics Provider submits a quote/service proposal for an export order.
     */
    @PostMapping
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<ProposalResponse>> submitProposal(
            @Valid @RequestBody ProposalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProposalResponse response = proposalService.submitProposal(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Proposal submitted successfully", response));
    }

    /**
     * Get all proposals for a specific order.
     * Accessible by the order's Exporter or a Logistics provider.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ProposalResponse>>> getProposalsForOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ProposalResponse> proposals = proposalService.getProposalsForOrder(orderId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(proposals));
    }

    /**
     * Logistics provider retrieves all proposals they have submitted.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<List<ProposalResponse>>> getMyProposals(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ProposalResponse> proposals = proposalService.getMyProposals(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(proposals));
    }

    /**
     * Exporter accepts a logistics quote/proposal.
     * Automatically creates a Shipment with tracking number and timeline history.
     */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> acceptProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProposalResponse response = proposalService.acceptProposal(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Proposal accepted. Shipment successfully booked!", response));
    }

    /**
     * Exporter rejects a logistics quote/proposal.
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> rejectProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProposalResponse response = proposalService.rejectProposal(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Proposal rejected.", response));
    }

    /**
     * Retrieve single proposal by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProposalResponse response = proposalService.getProposalById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
