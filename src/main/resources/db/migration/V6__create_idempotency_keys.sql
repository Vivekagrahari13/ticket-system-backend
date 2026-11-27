CREATE TABLE idempotency_keys (
                                  id           BIGSERIAL PRIMARY KEY,
                                  user_id      BIGINT       NOT NULL REFERENCES users (id),
                                  idem_key     VARCHAR(100) NOT NULL,
                                  request_hash VARCHAR(64)  NOT NULL,
                                  ticket_id    BIGINT       NOT NULL REFERENCES tickets (id),
                                  created_at   TIMESTAMP    NOT NULL DEFAULT now(),
                                  updated_at   TIMESTAMP,
                                  CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, idem_key)
);