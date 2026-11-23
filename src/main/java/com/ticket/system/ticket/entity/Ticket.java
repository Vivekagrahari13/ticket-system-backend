package com.ticket.system.ticket.entity;

import com.ticket.system.entity.GenericEntity;
import com.ticket.system.ticket.enums.Priority;
import com.ticket.system.ticket.enums.TicketStatus;
import com.ticket.system.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket extends GenericEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Version
    private Long version;
}