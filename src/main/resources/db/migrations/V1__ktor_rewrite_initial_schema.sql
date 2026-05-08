-- Jobs and related state
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

-- Initial state for jobs
insert into job(name, desired_state, updated_at, updated_by)
values ('SYKMELDING_CONSUMER', 'RUNNING', now(), 'system');

insert into job(name, desired_state, updated_at, updated_by)
values ('SYKMELDING_PRODUCER', 'RUNNING', now(), 'system');

insert into job(name, desired_state, updated_at, updated_by)
values ('SYKMELDING_DELETE', 'RUNNING', now(), 'system');

insert into job(name, desired_state, updated_at, updated_by)
values ('JURIDISK_PRODUCER', 'RUNNING', now(), 'system');

-- Actual sykmelding table, flattened out for easier maintenance when working with changes to the schema in the future
CREATE TABLE sykmelding
(
    id                                    UUID PRIMARY KEY,
    type                                  TEXT        NOT NULL,
    idempotency_key                       UUID UNIQUE NOT NULL,
    earliest_fom                          DATE        NOT NULL,
    latest_tom                            DATE        NOT NULL,
    validation_result                     JSONB       NOT NULL,
    meta_source                           TEXT        NOT NULL,
    meta_mottatt                          TIMESTAMPTZ NOT NULL,
    meta_pasient_ident                    TEXT        NOT NULL,
    meta_pasient_navn                     JSONB       NOT NULL,
    meta_behandler_hpr                    TEXT        NULL,
    meta_behandler_ident                  TEXT        NULL,
    meta_behandler_navn                   JSONB       NULL,
    meta_behandler_helsepersonellkategori JSONB       NULL,
    meta_orgnummer                        TEXT        NULL,
    meta_telefonnummer                    TEXT        NULL,
    values_pasienten_skal_skjermes        BOOLEAN     NOT NULL,
    values_svangerskapsrelatert           BOOLEAN     NOT NULL,
    values_hoveddiagnose                  JSONB       NULL,
    values_bidiagnoser                    JSONB       NULL,
    values_aktivitet                      JSONB       NOT NULL,
    values_meldinger                      JSONB       NULL,
    values_yrkesskade                     JSONB       NULL,
    values_arbeidsgiver                   JSONB       NULL,
    values_tilbakedatering                JSONB       NULL,
    values_utdypende_sporsmal             JSONB       NULL,
    values_annen_fravarsgrunn             TEXT        NULL
);

create index idx_sykmelding_latest_tom_date on sykmelding (latest_tom);
create index idx_sykmelding_latest_pasient_ident on sykmelding (meta_pasient_ident);

-- Sykmelding status are sykmeldinger that are to be published to kafka
create table sykmelding_status
(
    sykmelding_id     UUID primary key references sykmelding (id) on delete cascade,
    status            text        not null,
    rule_reason       jsonb       null,
    mottatt_timestamp timestamptz not null,
    event_timestamp   timestamptz not null,
    send_timestamp    timestamptz not null,
    source            text        not null
);

create index idx_sykmelding_status_event_timestamp on sykmelding_status (status, event_timestamp);
create index idx_sykmelding_status_sendt_timestamp on sykmelding_status (status, send_timestamp);

-- Juridisk status are "juridisk henvisninger" during rule execution that are to be published to the paragraf-i-kode topic
create table juridisk_status
(
    sykmelding_id      UUID primary key references sykmelding (id) on delete cascade,
    status             text        not null,
    event_timestamp    timestamptz not null,
    juridisk_vurdering jsonb       not null
);

create index idx_rule_status_event_timestamp on juridisk_status (status, event_timestamp);

-- Sykmeldinger that are explicitly marked as "poison pills"
create table sykmelding_poison_pill
(
    -- This is specifically NOT a foreign key, because these can't exist when they are read from kafka! :)
    sykmelding_id     UUID primary key,
    reason            text        not null,
    created timestamptz not null default now()
);
