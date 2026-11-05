package com.ticket.system.ticket.dto;

import com.ticket.system.ticket.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull TicketStatus status)
{}
