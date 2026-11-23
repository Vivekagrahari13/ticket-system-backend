CREATE TABLE ticket_status_history (
                                       id          BIGSERIAL PRIMARY KEY,
                                       ticket_id   BIGINT      NOT NULL REFERENCES tickets (id),
                                       from_status VARCHAR(20),
                                       to_status   VARCHAR(20) NOT NULL,
                                       changed_by  BIGINT      NOT NULL REFERENCES users (id),
                                       note        TEXT,
                                       created_at  TIMESTAMP   NOT NULL DEFAULT now(),
                                       updated_at  TIMESTAMP
);

CREATE INDEX idx_ticket_status_history_ticket ON ticket_status_history (ticket_id, created_at);