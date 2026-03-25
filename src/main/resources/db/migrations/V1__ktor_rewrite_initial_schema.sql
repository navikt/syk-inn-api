CREATE TABLE job
(
    name          TEXT PRIMARY KEY,
    desired_state TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    updated_by    TEXT        NOT NULL
);

CREATE TABLE job_status
(
    runner     TEXT        NOT NULL,
    job        TEXT        NOT NULL,
    state      TEXT        NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (runner, job)
);

insert into job(name, desired_state, updated_at, updated_by)
values ('SYKMELDING_CONSUMER', 'STOPPED', now(), 'system');

CREATE TABLE sykmelding
(
    id                             UUID PRIMARY KEY,
    rules                          JSONB       NOT NULL,
    meta_source                    TEXT        NOT NULL,
    meta_mottatt                   TIMESTAMPTZ NOT NULL,
    meta_pasient_ident             TEXT        NOT NULL,
    meta_behandler_hpr             TEXT        NOT NULL,
    meta_orgnummer                 TEXT        NOT NULL,
    meta_telefonnummer             TEXT        NOT NULL,
    values_pasienten_skal_skjermes BOOLEAN     NOT NULL,
    values_svangerskapsrelatert    BOOLEAN     NOT NULL,
    values_hoveddiagnose           JSONB       NULL,
    values_bidiagnoser             JSONB       NOT NULL,
    values_aktivitet               JSONB       NOT NULL,
    values_meldinger               JSONB       NULL,
    values_yrkesskade              JSONB       NULL,
    values_arbeidsgiver            JSONB       NULL,
    values_tilbakedatering         JSONB       NULL,
    values_utdypende_sporsmal      JSONB       NULL,
    values_annen_fravarsgrunn      TEXT        NULL
);
