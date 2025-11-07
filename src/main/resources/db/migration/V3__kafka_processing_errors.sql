CREATE TABLE kafka_processing_errors
(
    partition_offset text primary key NOT NULL,
    key              Text             NOT NULL,
    kafka_offset     BIGINT           NOT NULL,
    kafka_partition  INTEGER          NOT NULL,
    error            Text,
    stack_trace      Text
);
