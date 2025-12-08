ALTER TABLE sykmelding
    ADD COLUMN idempotency_key UUID;

UPDATE sykmelding
SET idempotency_key = gen_random_uuid()
WHERE idempotency_key IS NULL;

ALTER TABLE sykmelding
    ADD CONSTRAINT sykmelding_idempotency_key_uniq UNIQUE (idempotency_key);
