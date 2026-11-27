package com.ticket.system.ticket.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTest {

    @ParameterizedTest(name = "{0} to {1} allowed = {2}")
    @CsvSource({
            "OPEN, OPEN, false",
            "OPEN, IN_PROGRESS, true",
            "OPEN, CLOSED, true",
            "OPEN, WITHDRAWN, true",
            "IN_PROGRESS, OPEN, false",
            "IN_PROGRESS, IN_PROGRESS, false",
            "IN_PROGRESS, CLOSED, true",
            "IN_PROGRESS, WITHDRAWN, false",
            "CLOSED, OPEN, false",
            "CLOSED, IN_PROGRESS, false",
            "CLOSED, CLOSED, false",
            "CLOSED, WITHDRAWN, false",
            "WITHDRAWN, OPEN, false",
            "WITHDRAWN, IN_PROGRESS, false",
            "WITHDRAWN, CLOSED, false",
            "WITHDRAWN, WITHDRAWN, false"
    })
    void canMoveTo_followsTheForwardOnlyRules(TicketStatus from, TicketStatus to, boolean expected) {
        assertThat(from.canMoveTo(to)).isEqualTo(expected);
    }

    @Test
    void closedAndWithdrawn_areFinalAndNotEditable() {
        assertThat(TicketStatus.CLOSED.isFinal()).isTrue();
        assertThat(TicketStatus.WITHDRAWN.isFinal()).isTrue();
        assertThat(TicketStatus.CLOSED.isEditable()).isFalse();
        assertThat(TicketStatus.WITHDRAWN.isEditable()).isFalse();
    }

    @Test
    void canBeSetByAdmin_onlyForInProgressAndClosed() {
        assertThat(TicketStatus.IN_PROGRESS.canBeSetByAdmin()).isTrue();
        assertThat(TicketStatus.CLOSED.canBeSetByAdmin()).isTrue();
        assertThat(TicketStatus.OPEN.canBeSetByAdmin()).isFalse();
        assertThat(TicketStatus.WITHDRAWN.canBeSetByAdmin()).isFalse();
    }

    @Test
    void openAndInProgress_areNotFinalAndAreEditable() {
        assertThat(TicketStatus.OPEN.isFinal()).isFalse();
        assertThat(TicketStatus.IN_PROGRESS.isFinal()).isFalse();
        assertThat(TicketStatus.OPEN.isEditable()).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.isEditable()).isTrue();
    }
}