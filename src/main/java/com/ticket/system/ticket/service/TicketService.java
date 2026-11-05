package com.ticket.system.ticket.service;

import com.ticket.system.common.exception.ForbiddenException;
import com.ticket.system.common.exception.NotFoundException;
import com.ticket.system.common.exception.UnauthorizedException;
import com.ticket.system.security.CustomUserDetails;
import com.ticket.system.ticket.dto.CreateTicketRequest;
import com.ticket.system.ticket.dto.TicketResponse;
import com.ticket.system.ticket.dto.UpdateStatusRequest;
import com.ticket.system.ticket.entity.Ticket;
import com.ticket.system.ticket.enums.TicketStatus;
import com.ticket.system.ticket.repository.TicketRepository;
import com.ticket.system.user.entity.User;
import com.ticket.system.user.enums.Role;
import com.ticket.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

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

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    // USER sees only their own tickets, ADMIN sees all of them
    @Transactional(readOnly = true)
    public Page<TicketResponse> list(TicketStatus status, Pageable pageable, CustomUserDetails me) {
        Page<Ticket> page;
        if (isAdmin(me)) {
            page = (status == null)
                    ? ticketRepository.findAll(pageable)
                    : ticketRepository.findByStatus(status, pageable);
        } else {
            page = (status == null)
                    ? ticketRepository.findByOwnerId(me.getId(), pageable)
                    : ticketRepository.findByOwnerIdAndStatus(me.getId(), status, pageable);
        }
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

    // Admin only
    @Transactional
    public TicketResponse updateStatus(Long id, UpdateStatusRequest request, CustomUserDetails me) {
        requireAdmin(me, "Only admins can change a ticket's status");
        Ticket ticket = findTicket(id);
        ticket.setStatus(request.status());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    // Admin only
    @Transactional
    public void delete(Long id, CustomUserDetails me) {
        requireAdmin(me, "Only admins can delete a ticket");
        Ticket ticket = findTicket(id);
        ticketRepository.delete(ticket);
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
}
