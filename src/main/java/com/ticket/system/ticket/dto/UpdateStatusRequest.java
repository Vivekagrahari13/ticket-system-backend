package com.ticket.system.ticket.dto;

import com.ticket.system.ticket.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotNull TicketStatus status,
        @Size(max = 1000) String resolutionNote)
{}
