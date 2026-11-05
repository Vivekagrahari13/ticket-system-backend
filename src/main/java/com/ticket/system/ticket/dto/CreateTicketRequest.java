package com.ticket.system.ticket.dto;

import com.ticket.system.ticket.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank String description,
        @NotNull Priority priority) {}
