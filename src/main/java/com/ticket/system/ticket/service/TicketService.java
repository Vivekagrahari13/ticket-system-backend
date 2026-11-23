package com.ticket.system.ticket.service;

import com.ticket.system.common.exception.BadRequestException;
import com.ticket.system.common.exception.ConflictException;
import com.ticket.system.common.exception.ForbiddenException;
import com.ticket.system.common.exception.NotFoundException;
import com.ticket.system.common.exception.UnauthorizedException;
import com.ticket.system.security.CustomUserDetails;
import com.ticket.system.ticket.dto.*;
import com.ticket.system.ticket.entity.Ticket;
import com.ticket.system.ticket.enums.TicketStatus;
import com.ticket.system.ticket.repository.TicketRepository;
import com.ticket.system.user.entity.User;
import com.ticket.system.user.enums.Role;
import com.ticket.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticket.system.ticket.entity.TicketStatusHistory;
import com.ticket.system.ticket.repository.TicketStatusHistoryRepository;
import java.util.List;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketStatusHistoryRepository historyRepository;

    @Transactional
    public TicketResponse create(CreateTicketRequest request, CustomUserDetails me) {
        User owner = userRepository.findById(me.getId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setOwner(owner);

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} created by user {}", saved.getId(), me.getId());
        historyRepository.save(TicketStatusHistory.of(saved, null, TicketStatus.OPEN, owner, null));
        return TicketResponse.from(saved);
    }

    // Everyone, admins included, sees only the tickets they raised themselves
    @Transactional(readOnly = true)
    public Page<TicketResponse> listMine(TicketStatus status, Pageable pageable, CustomUserDetails me) {
        Page<Ticket> page = (status == null)
                ? ticketRepository.findByOwnerId(me.getId(), pageable)
                : ticketRepository.findByOwnerIdAndStatus(me.getId(), status, pageable);
        return page.map(TicketResponse::from);
    }

    // Admin only: every ticket from every user
    @Transactional(readOnly = true)
    public Page<TicketResponse> listAll(TicketStatus status, Pageable pageable, CustomUserDetails me) {
        requireAdmin(me, "Only admins can list all tickets");
        Page<Ticket> page = (status == null)
                ? ticketRepository.findAll(pageable)
                : ticketRepository.findByStatus(status, pageable);
        return page.map(TicketResponse::from);
    }

    // 404 if missing, 403 if the ticket is not theirs and they are not an admin
    @Transactional(readOnly = true)
    public TicketResponse get(Long id, CustomUserDetails me) {
        Ticket ticket = findTicket(id);
        if (!isAdmin(me) && !ticket.getOwner().getId().equals(me.getId())) {
            throw new ForbiddenException("You can't view this ticket");
        }
        return TicketResponse.from(ticket);
    }

    // Admin only. Order of checks: 403, then 400 (bad request), then 404, then 409.
    @Transactional
    public TicketResponse updateStatus(Long id, UpdateStatusRequest request, CustomUserDetails me) {
        requireAdmin(me, "Only admins can change a ticket's status");

        TicketStatus next = request.status();
        if (!next.canBeSetByAdmin()) {
            throw new BadRequestException("Status can only be changed to IN_PROGRESS or CLOSED");
        }
        String note = request.resolutionNote() == null ? "" : request.resolutionNote().trim();
        if (next == TicketStatus.CLOSED && note.isEmpty()) {
            throw new BadRequestException("A resolution note is required when closing a ticket");
        }

        Ticket ticket = findTicket(id);
        TicketStatus current = ticket.getStatus();
        if (!current.canMoveTo(next)) {
            throw new ConflictException("Cannot move a ticket from " + current + " to " + next);
        }

        ticket.setStatus(next);
        if (next == TicketStatus.CLOSED) {
            ticket.setResolutionNote(note);
            ticket.setClosedAt(LocalDateTime.now());
        }
        historyRepository.save(TicketStatusHistory.of(ticket, current, next,
                userRepository.getReferenceById(me.getId()),
                next == TicketStatus.CLOSED ? note : null));
        log.info("Ticket {} status changed from {} to {} by admin {}", id, current, next, me.getId());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
    }

    private boolean isAdmin(CustomUserDetails me) {
        return me.getRole() == Role.ADMIN;
    }

    private void requireAdmin(CustomUserDetails me, String message) {
        if (!isAdmin(me)) {
            throw new ForbiddenException(message);
        }
    }

    // Owner or admin, and only while the ticket is not closed or withdrawn
    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request, CustomUserDetails me) {
        Ticket ticket = findTicket(id);

        // TEMP-REPRO: For testing purpose
//        log.info("TEMP-REPRO: loaded ticket {} with status {}, sleeping 10s", id, ticket.getStatus());
//        try {
//            Thread.sleep(10_000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
        // END TEMP-REPRO

        if (!isAdmin(me) && !ticket.getOwner().getId().equals(me.getId())) {
            throw new ForbiddenException("You can't edit this ticket");
        }
        if (!ticket.getStatus().isEditable()) {
            throw new ConflictException("A closed or withdrawn ticket can't be edited");
        }

        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        log.info("Ticket {} edited by user {}", id, me.getId());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    // Owner only, and only while the ticket is still open
    @Transactional
    public TicketResponse withdraw(Long id, CustomUserDetails me) {
        Ticket ticket = findTicket(id);

        if (!ticket.getOwner().getId().equals(me.getId())) {
            throw new ForbiddenException("Only the ticket owner can withdraw a ticket");
        }
        if (!ticket.getStatus().canMoveTo(TicketStatus.WITHDRAWN)) {
            throw new ConflictException("Only an open ticket can be withdrawn");
        }

        TicketStatus current = ticket.getStatus();
        ticket.setStatus(TicketStatus.WITHDRAWN);
        ticket.setWithdrawnAt(LocalDateTime.now());
        historyRepository.save(TicketStatusHistory.of(ticket, current, TicketStatus.WITHDRAWN,
                userRepository.getReferenceById(me.getId()), null));
        log.info("Ticket {} withdrawn by owner {}", id, me.getId());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> history(Long id, CustomUserDetails me) {
        Ticket ticket = findTicket(id);
        if (!isAdmin(me) && !ticket.getOwner().getId().equals(me.getId())) {
            throw new ForbiddenException("You can't view this ticket's history");
        }
        return historyRepository.findByTicketIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(TicketHistoryResponse::from)
                .toList();
    }
}
