package com.ticket.system.ticket.dto;

import com.ticket.system.auth.dto.UserResponse;
import com.ticket.system.ticket.entity.Ticket;
import com.ticket.system.ticket.enums.Priority;
import com.ticket.system.ticket.enums.TicketStatus;
import com.ticket.system.user.entity.User;

import java.time.LocalDateTime;

public record TicketResponse(Long id,
                             String title,
                             String description,
                             Priority priority,
                             TicketStatus status,
                             LocalDateTime createdAt,
                             String resolutionNote,
                             LocalDateTime closedAt,
                             LocalDateTime withdrawnAt,
                             UserResponse owner)
{
    public static TicketResponse from(Ticket ticket) {
        User owner = ticket.getOwner();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getResolutionNote(),
                ticket.getClosedAt(),
                ticket.getWithdrawnAt(),
                new UserResponse(owner.getId(), owner.getEmail(), owner.getRole()));
    }
}
