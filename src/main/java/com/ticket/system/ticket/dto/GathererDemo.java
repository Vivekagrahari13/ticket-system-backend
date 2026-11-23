package com.ticket.system.ticket.dto;

import module java.base;

public class GathererDemo {

    enum TicketStatus {
        OPEN, IN_PROGRESS, CLOSED, WITHDRAWN;
        boolean canMoveTo(TicketStatus next) {
            return switch (this) {
                case OPEN -> next == IN_PROGRESS || next == CLOSED || next == WITHDRAWN;
                case IN_PROGRESS -> next == CLOSED;
                case CLOSED, WITHDRAWN -> false;
            };
        }
    }

    public static void main(String[] args) {
        // 1. History of one ticket: check every consecutive pair is a legal move.
        //    This is what a Day 3 status-history audit would look like.
        List<TicketStatus> history = List.of(
                TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.CLOSED, TicketStatus.OPEN);

        history.stream()
                .gather(Gatherers.windowSliding(3))
                .filter(w -> w.size() == 3)      // a 1-element history gives one partial window
                .forEach(w -> System.out.println(
                        w.get(0) + " -> " + w.get(1) + "  valid? " + w.get(0).canMoveTo(w.get(1))));

        // 2. Batches of ticket ids for batch processing
        IntStream.rangeClosed(1, 7).boxed()
                .gather(Gatherers.windowFixed(1))
                .forEach(batch -> System.out.println("batch " + batch));

        // 3. Running total: tickets closed per day, cumulative
        Stream.of(2, 0, 3, 1)
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .forEach(total -> System.out.println("closed so far " + total));
    }
}
