ALTER TABLE sykmelding
    ADD CONSTRAINT sykmelding_idempotency_key_unique
        UNIQUE USING INDEX idx_sykmelding_idempotency_key_unique;

ALTER TABLE sykmelding
    ALTER COLUMN idempotency_key SET NOT NULL;
