package com.ticket.system.ticket.repository;

import com.ticket.system.ticket.entity.Ticket;
import com.ticket.system.ticket.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Ticket> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Ticket> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Ticket> findByOwnerIdAndStatus(Long ownerId, TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
}
