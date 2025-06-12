CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE sykmelding
(
    id               UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    sykmelding_id    TEXT UNIQUE NOT NULL,
    mottatt          timestamptz NOT NULL,
    pasient_ident    TEXT        NOT NULL,
    sykmelder_hpr    TEXT        NOT NULL,
    sykmelding       JSONB       NOT NULL,
    legekontor_orgnr TEXT        NOT NULL,
    legekontor_tlf   TEXT,
    validert_ok      BOOLEAN     NOT NULL DEFAULT FALSE
);
