package com.trade.repository;

import com.trade.entity.LogisticsProposal;
import com.trade.entity.Order;
import com.trade.entity.ProposalStatus;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogisticsProposalRepository extends JpaRepository<LogisticsProposal, Long> {

    List<LogisticsProposal> findByOrder(Order order);

    @EntityGraph(attributePaths = {
            "order", "order.product", "order.destinationCountry",
            "logisticsPartner", "logisticsPartner.logisticsProfile"
    })
    List<LogisticsProposal> findByOrderOrderByCreatedAtDesc(Order order);

    @EntityGraph(attributePaths = {
            "order", "order.product", "order.destinationCountry",
            "logisticsPartner", "logisticsPartner.logisticsProfile"
    })
    List<LogisticsProposal> findByLogisticsPartnerOrderByCreatedAtDesc(User logisticsPartner);

    Optional<LogisticsProposal> findByOrderAndLogisticsPartner(Order order, User logisticsPartner);

    boolean existsByOrderAndLogisticsPartnerAndStatus(Order order, User logisticsPartner, ProposalStatus status);

    long countByLogisticsPartnerAndStatus(User logisticsPartner, ProposalStatus status);

    long countByLogisticsPartner(User logisticsPartner);
}
