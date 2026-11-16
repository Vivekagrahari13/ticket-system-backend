package com.ticket.system.ticket.controller;

import com.ticket.system.security.CustomUserDetails;
import com.ticket.system.ticket.dto.TicketResponse;
import com.ticket.system.ticket.enums.TicketStatus;
import com.ticket.system.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Admin-only views. The role check is enforced in the service.
@RestController
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketController {

    private final TicketService ticketService;

    @GetMapping
    public PagedModel<TicketResponse> listAll(
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails me) {
        return new PagedModel<>(ticketService.listAll(status, pageable, me));
    }
}
