package com.ticket.system.ticket.dto;

import com.ticket.system.ticket.entity.TicketStatusHistory;
import com.ticket.system.ticket.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketHistoryResponse(
        Long id,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        Long changedBy,
        String note,
        LocalDateTime changedAt) {

    public static TicketHistoryResponse from(TicketStatusHistory h) {
        return new TicketHistoryResponse(
                h.getId(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getChangedBy().getId(),
                h.getNote(),
                h.getCreatedAt());
    }
}