CREATE UNIQUE INDEX CONCURRENTLY if not exists idx_sykmelding_idempotency_key_unique
    ON sykmelding (idempotency_key);

DROP INDEX concurrently if exists idx_sykmelding_idempotency_key;
