package com.ticket.system.ticket.entity;

import com.ticket.system.entity.GenericEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord extends GenericEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idem_key", nullable = false, length = 100)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    public static IdempotencyRecord of(Long userId, String key, String hash, Long ticketId) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.setUserId(userId);
        r.setIdemKey(key);
        r.setRequestHash(hash);
        r.setTicketId(ticketId);
        return r;
    }
}
