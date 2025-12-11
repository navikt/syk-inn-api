CREATE INDEX CONCURRENTLY if not exists idx_sykmelding_idempotency_key
    ON sykmelding (idempotency_key);
