create table rule_status
(
    sykmelding_id        UUID primary key,
    status               text        not null,
    event_timestamp      timestamptz not null,
    juridiskHenvisning   jsonb       not null
);

create index idx_rule_status_event_timestamp on rule_status (status, event_timestamp);
