CREATE TABLE job
(
    name          TEXT PRIMARY KEY,
    desired_state TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    updated_by    TEXT        NOT NULL
);

CREATE TABLE job_status
(
    runner        TEXT        NOT NULL,
    job           TEXT        NOT NULL,
    state         TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (runner, job)
);

insert into job(name, desired_state, updated_at, updated_by) values ('SYKMELDING_CONSUMER', 'STOPPED', now(), 'system');

CREATE TABLE sykmelding
(
    id         UUID PRIMARY KEY,
    data       JSONB       NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
