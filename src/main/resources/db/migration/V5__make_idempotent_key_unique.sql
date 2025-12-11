
ALTER TABLE sykmelding
    ADD CONSTRAINT sykmelding_idempotency_key_uniq UNIQUE (idempotency_key);
