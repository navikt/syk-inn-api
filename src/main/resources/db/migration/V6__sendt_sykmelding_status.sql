create table sykmelding_status (
                                   sykmelding_id UUID primary key,
                                   status text not null,
                                   mottatt_timestamp timestamptz not null ,
                                   event_timestamp timestamptz not null
);

create index idx_sykmelding_status_event_timestamp on sykmelding_status (status, event_timestamp);
