CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE sykmelding
(
    id               UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    sykmelding_id    TEXT UNIQUE NOT NULL,
    mottatt          timestamptz NOT NULL,
    pasient_ident    TEXT        NOT NULL,
    sykmelder_hpr    TEXT        NOT NULL,
    sykmelding       JSONB       NOT NULL,
    legekontor_orgnr TEXT,
    legekontor_tlf   TEXT,
    validert_ok      BOOLEAN     NOT NULL DEFAULT FALSE,
    fom              DATE,
    tom              DATE
);

CREATE INDEX idx_sykmelding_pasient_ident ON sykmelding (pasient_ident);
CREATE INDEX idx_sykmelding_fom ON sykmelding (fom);
CREATE INDEX idx_sykmelding_tom ON sykmelding (tom);
