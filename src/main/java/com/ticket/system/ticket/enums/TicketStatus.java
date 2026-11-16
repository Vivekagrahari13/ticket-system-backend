package com.ticket.system.ticket.enums;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED,
    WITHDRAWN;

    // Status only moves forward. CLOSED and WITHDRAWN are final.
    public boolean canMoveTo(TicketStatus next) {
        return switch (this) {
            case OPEN -> next == IN_PROGRESS || next == CLOSED || next == WITHDRAWN;
            case IN_PROGRESS -> next == CLOSED;
            case CLOSED, WITHDRAWN -> false;
        };
    }

    public boolean isFinal() {
        return this == CLOSED || this == WITHDRAWN;
    }

    // Title, description and priority can be edited only while the ticket is not final.
    public boolean isEditable() {
        return !isFinal();
    }

    // An admin can only start work or close. Withdrawing belongs to the owner.
    public boolean canBeSetByAdmin() {
        return this == IN_PROGRESS || this == CLOSED;
    }
}
