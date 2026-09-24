package com.trade.service;

import com.trade.dto.proposal.ProposalRequest;
import com.trade.dto.proposal.ProposalResponse;

import java.util.List;

public interface LogisticsProposalService {

    ProposalResponse submitProposal(ProposalRequest request, String partnerEmail);

    List<ProposalResponse> getProposalsForOrder(Long orderId, String userEmail);

    List<ProposalResponse> getMyProposals(String partnerEmail);

    ProposalResponse acceptProposal(Long proposalId, String exporterEmail);

    ProposalResponse rejectProposal(Long proposalId, String exporterEmail);

    ProposalResponse getProposalById(Long proposalId, String userEmail);
}
