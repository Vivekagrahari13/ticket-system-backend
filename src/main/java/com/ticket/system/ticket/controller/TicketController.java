package com.ticket.system.ticket.controller;

import com.ticket.system.security.CustomUserDetails;
import com.ticket.system.ticket.dto.CreateTicketRequest;
import com.ticket.system.ticket.dto.TicketResponse;
import com.ticket.system.ticket.dto.UpdateStatusRequest;
import com.ticket.system.ticket.enums.TicketStatus;
import com.ticket.system.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody @Valid CreateTicketRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request, me));
    }

    @GetMapping
    public PagedModel<TicketResponse> list(
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails me) {
        return new PagedModel<>(ticketService.list(status, pageable, me));
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        return ticketService.get(id, me);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable Long id,
                                       @RequestBody @Valid UpdateStatusRequest request,
                                       @AuthenticationPrincipal CustomUserDetails me) {
        return ticketService.updateStatus(id, request, me);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails me) {
        ticketService.delete(id, me);
        return ResponseEntity.noContent().build();
    }
}
