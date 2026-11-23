package com.ticket.system.ticket.dto;

public class FlexibleCtorDemo {

    enum Priority {LOW, MEDIUM, HIGH}

    static class Ticket {
        final String title;
        final Priority priority;

        Ticket(String title, Priority priority) {
            this.title = title;
            this.priority = priority;
        }
    }

    static class UrgentTicket extends Ticket {
        UrgentTicket(String rawTitle) {
            // Java 25: statements are allowed before super(), as long as they don't use `this`
            if (rawTitle == null || rawTitle.isBlank())
                throw new IllegalArgumentException("title required");
            String clean = rawTitle.trim();
            super(clean, Priority.HIGH);
        }
    }

    public static void main(String[] args) {
        var t = new UrgentTicket("  Server down  ");
        System.out.println(t.title + " / " + t.priority);        // Server down / HIGH
        new UrgentTicket("   ");                                  // throws before super() runs
    }
}
