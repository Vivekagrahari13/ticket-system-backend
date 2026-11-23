//package com.ticket.system.ticket.dto;

import module java.base;   // module import declaration, final in Java 25

public class TicketPlayground {

    enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    record Ticket(long id, String title, Status status, String owner) {}

    sealed interface StatusChangeResult {
        record Changed(Ticket ticket, Status from, Status to) implements StatusChangeResult {}
        record Rejected(String reason) implements StatusChangeResult {}
        record NotFound(long id) implements StatusChangeResult {}
    }

    static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();

    static StatusChangeResult changeStatus(Map<Long, Ticket> store, long id, Status to) {
        Ticket t = store.get(id);
        if (t == null) return new StatusChangeResult.NotFound(id);
        if (!t.owner().equals(CURRENT_USER.get()))
            return new StatusChangeResult.Rejected(CURRENT_USER.get() + " is not the owner");
        if (t.status() == Status.CLOSED)
            return new StatusChangeResult.Rejected("ticket already closed");
        store.put(id, new Ticket(id, t.title(), to, t.owner()));
        return new StatusChangeResult.Changed(store.get(id), t.status(), to);
    }

    static String describe(StatusChangeResult r) {
        return switch (r) {
            case StatusChangeResult.Changed c  -> "Changed " + c.from() + " -> " + c.to();
            case StatusChangeResult.Rejected x -> "Rejected: " + x.reason();
            case StatusChangeResult.NotFound n -> "Ticket " + n.id() + " not found";
        };
    }

    public static void main(String[] args) {
        var store = new ConcurrentHashMap<Long, Ticket>();
        store.put(1L, new Ticket(1, "Login broken", Status.OPEN, "asha"));
        store.put(2L, new Ticket(2, "Slow report", Status.CLOSED, "ravi"));
//        store.put(3L, new Ticket(2, "BAd Health", Status.IN_PROGRESS, "ankit"));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var user : List.of("asha", "ravi", "asha")) {
                executor.submit(() -> ScopedValue.where(CURRENT_USER, user).run(() -> {
                    System.out.println(user + " -> " + describe(changeStatus(store, 1, Status.IN_PROGRESS)));
                    System.out.println(user + " -> " + describe(changeStatus(store, 2, Status.RESOLVED)));
                }));
            }
        }
    }
}
