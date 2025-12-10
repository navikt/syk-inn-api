# Sykmelding Metrics Documentation

This document describes the metrics instrumentation for the sykmelding application, including the metrics catalog, Service Level Indicators (SLIs), Service Level Objectives (SLOs), and Service Level Agreements (SLAs).

## Table of Contents

- [SLI/SLO/SLA Definitions](#slislosla-definitions)
- [Metrics Catalog](#metrics-catalog)
- [Grafana Dashboard Queries](#grafana-dashboard-queries)
- [Alerting Rules](#alerting-rules)
- [Troubleshooting](#troubleshooting)

## SLI/SLO/SLA Definitions

### Service Level Indicators (SLIs)

SLIs are quantitative measures of the level of service provided to users:

1. **Availability**: Percentage of successful requests
   - Metric: `(sykmelding.sli.requests.successful / sykmelding.sli.requests.total) * 100`
   
2. **Latency**: Response time for create and verify operations
   - Create p95: 95th percentile of `sykmelding.create.duration`
   - Verify p95: 95th percentile of `sykmelding.verify.duration`
   
3. **Error Rate**: Percentage of failed operations
   - Metric: `(sykmelding.sli.requests.failed / sykmelding.sli.requests.total) * 100`
   
4. **Consumer Freshness**: Time since last successful Kafka message processing
   - Metric: `(current_time - sykmelding.consumer.last.processing.timestamp)`

### Service Level Objectives (SLOs)

SLOs are target values for SLIs that we commit to achieving:

| SLI | SLO Target | Rationale |
|-----|------------|-----------|
| Availability | ≥99.5% | Critical health service - minimal downtime acceptable |
| Create Latency (p95) | ≤2000ms | Doctor workflow should not be blocked by slow creation |
| Verify Latency (p95) | ≤500ms | Real-time validation feedback for doctor |
| Error Rate | ≤1% | High reliability required for health data |
| Consumer Lag | ≤60s | Near real-time processing of sykmeldinger |

### Service Level Agreement (SLA)

The SLA is the formal commitment to users:

- **Uptime**: 99.0% per month (allows ~7.3 hours downtime per month)
- **Consequences**: SLA violations trigger:
  - Incident review and root cause analysis
  - Operational improvements to prevent recurrence
  - Escalation to management for repeated violations

The SLA is intentionally set slightly lower than SLOs (99.0% vs 99.5%) to provide an error budget for planned maintenance and unexpected issues.

## Metrics Catalog

### Operation Metrics

#### Sykmelding Creation
- `sykmelding.created` (Counter)
  - Tags: `source`, `diagnose_system`, `validation_result`, `aktivitet_type`, `yrkesskade`, `svangerskapsrelatert`, `tilbakedatering_present`
  - Description: Total number of sykmeldinger created
  
- `sykmelding.creation.failed` (Counter)
  - Tags: `error_type`, `source`
  - Description: Total number of failed sykmelding creations
  - Error types: `ResourceError`, `PersonDoesNotExist`, `PersistenceError`

- `sykmelding.create.duration` (Timer)
  - Tags: `source`
  - Description: Time taken to create a sykmelding (includes validation, persistence, and Kafka send)

#### Sykmelding Verification
- `sykmelding.verified` (Counter)
  - Tags: `source`, `regula_status`
  - Description: Total number of sykmeldinger verified (dry-run validation)

- `sykmelding.verification.failed` (Counter)
  - Tags: `error_type`, `source`
  - Description: Total number of failed verifications

- `sykmelding.verify.duration` (Timer)
  - Tags: `source`
  - Description: Time taken to verify a sykmelding

#### Sykmelding Updates & Deletions
- `sykmelding.updated` (Counter)
  - Tags: `sykmelding_type` (DIGITAL, PAPIR, XML, UTENLANDSK)
  - Description: Total number of sykmeldinger updated from Kafka

- `sykmelding.deleted` (Counter)
  - Description: Total number of sykmeldinger deleted (tombstones)

- `sykmelding.duration.days` (Summary)
  - Description: Distribution of sykmelding duration in days (tom - fom)

### Database Metrics

- `sykmelding.database.save` (Counter)
  - Description: Total number of database save operations

- `sykmelding.database.save.duration` (Timer)
  - Description: Time taken to save to database

- `sykmelding.database.query.duration` (Timer)
  - Tags: `operation` (getSykmeldingById, getSykmeldingerByIdent)
  - Description: Time taken for database queries

### Kafka Consumer Metrics

- `sykmelding.kafka.message.consumed` (Counter)
  - Description: Total number of Kafka messages consumed

- `sykmelding.kafka.tombstone.processed` (Counter)
  - Description: Total number of tombstone messages processed

- `sykmelding.kafka.processing.error` (Counter)
  - Tags: `exception_type` (jackson_error, pdl_error, hpr_error, unknown_error)
  - Description: Total number of Kafka processing errors

- `sykmelding.kafka.poison.pill` (Counter)
  - Description: Total number of poison pills encountered

- `sykmelding.kafka.processing.duration` (Timer)
  - Description: Time taken to process a Kafka message

- `sykmelding.consumer.last.processing.timestamp` (Gauge)
  - Description: Unix timestamp of last successful message processing (for lag calculation)

### Kafka Producer Metrics

- `sykmelding.kafka.message.sent` (Counter)
  - Tags: `source`
  - Description: Total number of Kafka messages sent

- `sykmelding.kafka.message.send.failed` (Counter)
  - Tags: `source`
  - Description: Total number of failed Kafka message sends

- `sykmelding.kafka.send.duration` (Timer)
  - Tags: `source`
  - Description: Time taken to send a Kafka message

### HTTP API Metrics

- `sykmelding.http.request` (Counter)
  - Tags: `endpoint`, `method`, `status_code`
  - Description: Total number of HTTP requests
  - Endpoints: `/api/sykmelding`, `/api/sykmelding/verify`, `/api/sykmelding/{id}`

- `sykmelding.http.request.duration` (Timer)
  - Tags: `endpoint`, `method`
  - Description: Time taken to process HTTP requests

- `sykmelding.access.control.denied` (Counter)
  - Tags: `endpoint`
  - Description: Total number of access control denials

### Scheduled Task Metrics

- `sykmelding.scheduled.deletion` (Counter)
  - Description: Total number of sykmeldinger deleted by scheduled task (counts individual deletions)

- `sykmelding.scheduled.deletion.duration` (Timer)
  - Description: Time taken for scheduled deletion task

- `sykmelding.scheduled.deletion.failure` (Counter)
  - Description: Total number of scheduled deletion failures

- `sykmelding.scheduled.deletion.last.run.timestamp` (Gauge)
  - Description: Unix timestamp of last successful scheduled deletion

### Error Handler Metrics

- `sykmelding.consumer.error.handled` (Counter)
  - Tags: `exception_type`
  - Description: Total number of consumer errors handled by error handler

- `sykmelding.consumer.retry.attempt` (Counter)
  - Description: Total number of consumer retry attempts

### Gauge Metrics (Updated Every 60s)

- `sykmelding.active.count` (Gauge)
  - Description: Current number of active sykmeldinger in database

- `sykmelding.validated.ok.count` (Gauge)
  - Description: Number of sykmeldinger with validertOk=true

- `sykmelding.validated.not.ok.count` (Gauge)
  - Description: Number of sykmeldinger with validertOk=false

- `sykmelding.error.queue.size` (Gauge)
  - Description: Current number of unresolved Kafka processing errors

- `sykmelding.oldest.age.days` (Gauge)
  - Description: Age in days of the oldest sykmelding (based on fom date)

- `sykmelding.oldest.error.age.hours` (Gauge)
  - Description: Age in hours of the oldest unresolved error

### Rule Validation Metrics

- `sykmelding.rule.validation.duration` (Timer)
  - Description: Time taken to validate rules using Regula

### SLI Metrics

- `sykmelding.sli.availability.percent` (Gauge)
  - Description: Current availability percentage

- `sykmelding.sli.error.rate.percent` (Gauge)
  - Description: Current error rate percentage

- `sykmelding.sli.consumer.lag.seconds` (Gauge)
  - Description: Current consumer lag in seconds

### SLO Violation Counters

- `sykmelding.slo.availability.violation` (Counter)
  - Description: Number of times availability fell below 99.5%

- `sykmelding.slo.latency.create.violation` (Counter)
  - Description: Number of create operations exceeding 2000ms

- `sykmelding.slo.latency.verify.violation` (Counter)
  - Description: Number of verify operations exceeding 500ms

- `sykmelding.slo.error.rate.violation` (Counter)
  - Description: Number of times error rate exceeded 1%

- `sykmelding.slo.consumer.lag.violation` (Counter)
  - Description: Number of times consumer lag exceeded 60s

## Grafana Dashboard Queries

### Availability Dashboard Panel

```promql
# Current availability (last 5 minutes)
(
  sum(rate(sykmelding_sli_requests_successful_total[5m])) / 
  sum(rate(sykmelding_sli_requests_total[5m]))
) * 100

# Availability over time (graph)
(
  sum(rate(sykmelding_sli_requests_successful_total[5m])) / 
  sum(rate(sykmelding_sli_requests_total[5m]))
) * 100

# SLO threshold line
99.5
```

### Latency Dashboard Panel

```promql
# Create latency p95 (last 5 minutes)
histogram_quantile(0.95, sum(rate(sykmelding_create_duration_seconds_bucket[5m])) by (le)) * 1000

# Verify latency p95 (last 5 minutes)
histogram_quantile(0.95, sum(rate(sykmelding_verify_duration_seconds_bucket[5m])) by (le)) * 1000

# SLO threshold lines
2000  # Create
500   # Verify
```

### Error Rate Dashboard Panel

```promql
# Overall error rate (last 5 minutes)
(
  sum(rate(sykmelding_sli_requests_failed_total[5m])) / 
  sum(rate(sykmelding_sli_requests_total[5m]))
) * 100

# Error rate by operation
(
  sum(rate(sykmelding_sli_requests_failed_total[5m])) by (operation) / 
  sum(rate(sykmelding_sli_requests_total[5m])) by (operation)
) * 100

# SLO threshold line
1.0
```

### Consumer Lag Dashboard Panel

```promql
# Current consumer lag
time() - sykmelding_consumer_last_processing_timestamp

# SLO threshold line
60
```

### Throughput Dashboard Panel

```promql
# Sykmeldinger created per minute
sum(rate(sykmelding_created_total[1m])) * 60

# Messages consumed per minute
sum(rate(sykmelding_kafka_message_consumed_total[1m])) * 60

# HTTP requests per minute
sum(rate(sykmelding_http_request_total[1m])) by (endpoint, method) * 60
```

### Error Breakdown Dashboard Panel

```promql
# Creation errors by type
sum(rate(sykmelding_creation_failed_total[5m])) by (error_type)

# Kafka processing errors by type
sum(rate(sykmelding_kafka_processing_error_total[5m])) by (exception_type)

# HTTP errors by endpoint
sum(rate(sykmelding_http_request_total{status_code=~"4..|5.."}[5m])) by (endpoint, status_code)
```

### Database Performance Dashboard Panel

```promql
# Database query latency p95
histogram_quantile(0.95, sum(rate(sykmelding_database_query_duration_seconds_bucket[5m])) by (le, operation)) * 1000

# Database save latency p95
histogram_quantile(0.95, sum(rate(sykmelding_database_save_duration_seconds_bucket[5m])) by (le)) * 1000
```

### Business Metrics Dashboard Panel

```promql
# Active sykmeldinger (current count)
sykmelding_active_count

# Sykmeldinger by diagnosis system
sum(rate(sykmelding_created_total[1h])) by (diagnose_system)

# Sykmeldinger by aktivitet type
sum(rate(sykmelding_created_total[1h])) by (aktivitet_type)

# Sykmeldinger with yrkesskade
sum(rate(sykmelding_created_total{yrkesskade="true"}[1h]))

# Average sykmelding duration
avg(sykmelding_duration_days)
```

### SLO Violations Dashboard Panel

```promql
# Total SLO violations in last 24h
sum(increase(sykmelding_slo_availability_violation_total[24h]))
+ sum(increase(sykmelding_slo_latency_create_violation_total[24h]))
+ sum(increase(sykmelding_slo_latency_verify_violation_total[24h]))
+ sum(increase(sykmelding_slo_error_rate_violation_total[24h]))
+ sum(increase(sykmelding_slo_consumer_lag_violation_total[24h]))

# Violations by type (graph)
sum(increase(sykmelding_slo_availability_violation_total[5m])) or vector(0)
sum(increase(sykmelding_slo_latency_create_violation_total[5m])) or vector(0)
sum(increase(sykmelding_slo_latency_verify_violation_total[5m])) or vector(0)
sum(increase(sykmelding_slo_error_rate_violation_total[5m])) or vector(0)
sum(increase(sykmelding_slo_consumer_lag_violation_total[5m])) or vector(0)
```

## Alerting Rules

### Critical Alerts (Immediate Action Required)

#### Availability SLO Violation
```yaml
alert: SykmeldingAvailabilityBelowSLO
expr: |
  (
    sum(rate(sykmelding_sli_requests_successful_total[5m])) / 
    sum(rate(sykmelding_sli_requests_total[5m]))
  ) * 100 < 99.5
for: 5m
labels:
  severity: critical
  team: syk-inn
annotations:
  summary: "Sykmelding availability below SLO (< 99.5%)"
  description: "Current availability: {{ $value | humanizePercentage }}"
  runbook: https://github.com/navikt/syk-inn-api/blob/main/README-METRICS.md#availability-below-slo
```

#### Consumer Lag Critical
```yaml
alert: SykmeldingConsumerLagCritical
expr: (time() - sykmelding_consumer_last_processing_timestamp) > 300
for: 2m
labels:
  severity: critical
  team: syk-inn
annotations:
  summary: "Sykmelding consumer lag exceeds 5 minutes"
  description: "Consumer has not processed messages for {{ $value }} seconds"
  runbook: https://github.com/navikt/syk-inn-api/blob/main/README-METRICS.md#consumer-lag
```

#### Error Queue Growing
```yaml
alert: SykmeldingErrorQueueGrowing
expr: sykmelding_error_queue_size > 100
for: 10m
labels:
  severity: warning
  team: syk-inn
annotations:
  summary: "Sykmelding error queue has {{ $value }} unresolved errors"
  description: "Error queue growing - investigate processing failures"
  runbook: https://github.com/navikt/syk-inn-api/blob/main/README-METRICS.md#error-queue
```

### Warning Alerts (Investigation Required)

#### Error Rate SLO Violation
```yaml
alert: SykmeldingErrorRateHigh
expr: |
  (
    sum(rate(sykmelding_sli_requests_failed_total[5m])) / 
    sum(rate(sykmelding_sli_requests_total[5m]))
  ) * 100 > 1.0
for: 10m
labels:
  severity: warning
  team: syk-inn
annotations:
  summary: "Sykmelding error rate above SLO (> 1%)"
  description: "Current error rate: {{ $value | humanizePercentage }}"
  runbook: https://github.com/navikt/syk-inn-api/blob/main/README-METRICS.md#error-rate-high
```

#### Create Latency SLO Violation
```yaml
alert: SykmeldingCreateLatencyHigh
expr: |
  histogram_quantile(0.95, 
    sum(rate(sykmelding_create_duration_seconds_bucket[5m])) by (le)
  ) * 1000 > 2000
for: 10m
labels:
  severity: warning
  team: syk-inn
annotations:
  summary: "Sykmelding create latency p95 exceeds SLO (> 2000ms)"
  description: "Current p95 latency: {{ $value | humanize }}ms"
  runbook: https://github.com/navikt/syk-inn-api/blob/main/README-METRICS.md#latency-high
```

#### Database Performance Degradation
```yaml
alert: SykmeldingDatabaseSlowQueries
expr: |
  histogram_quantile(0.95, 
    sum(rate(sykmelding_database_query_duration_seconds_bucket[5m])) by (le, operation)
  ) * 1000 > 1000
for: 10m
labels:
  severity: warning
  team: syk-inn
annotations:
  summary: "Sykmelding database queries slow (p95 > 1000ms)"
  description: "Operation {{ $labels.operation }} p95: {{ $value | humanize }}ms"
  runbook: https://github.com/navikt/syk-inn-api/blob/main/README-METRICS.md#database-slow
```

### Info Alerts (Monitoring)

#### No Traffic
```yaml
alert: SykmeldingNoTraffic
expr: sum(rate(sykmelding_http_request_total[5m])) == 0
for: 15m
labels:
  severity: info
  team: syk-inn
annotations:
  summary: "No HTTP traffic to sykmelding API"
  description: "No requests received in last 15 minutes"
```

#### Scheduled Deletion Failed
```yaml
alert: SykmeldingScheduledDeletionFailed
expr: increase(sykmelding_scheduled_deletion_failure_total[1h]) > 0
labels:
  severity: warning
  team: syk-inn
annotations:
  summary: "Scheduled deletion task failed"
  description: "Old sykmeldinger cleanup failed - manual intervention may be required"
```

## Troubleshooting

### Availability Below SLO

**Symptoms**: `sykmelding.sli.availability.percent < 99.5%`

**Investigation Steps**:
1. Check error rate by operation:
   ```promql
   sum(rate(sykmelding_sli_requests_failed_total[5m])) by (operation)
   ```
2. Check HTTP error codes:
   ```promql
   sum(rate(sykmelding_http_request_total{status_code=~"5.."}[5m])) by (endpoint)
   ```
3. Check external service errors (PDL, HPR):
   ```promql
   sum(rate(sykmelding_creation_failed_total{error_type="ResourceError"}[5m]))
   ```
4. Review application logs for exceptions
5. Check database connection pool health

**Common Causes**:
- External service (PDL/HPR) unavailable
- Database connection issues
- Kafka producer failures

**Resolution**:
- If external services down: Enable circuit breaker, notify platform team
- If database issues: Check CloudSQL status, connection pool settings
- If Kafka issues: Check Kafka cluster health with NAIS team

### Consumer Lag

**Symptoms**: `(time() - sykmelding_consumer_last_processing_timestamp) > 60`

**Investigation Steps**:
1. Check consumer error rate:
   ```promql
   sum(rate(sykmelding_kafka_processing_error_total[5m])) by (exception_type)
   ```
2. Check poison pills:
   ```promql
   sum(rate(sykmelding_kafka_poison_pill_total[5m]))
   ```
3. Check consumer processing duration:
   ```promql
   histogram_quantile(0.95, sum(rate(sykmelding_kafka_processing_duration_seconds_bucket[5m])) by (le))
   ```
4. Check Kafka topic lag in NAIS console

**Common Causes**:
- Poison pill message blocking consumer
- External service (PDL/HPR) slow/unavailable
- Database write contention
- Consumer crashed/restarting

**Resolution**:
- Poison pill: Add to poison pills list, restart consumer
- External service slow: Temporary - wait for recovery
- Database issues: Check slow queries, connection pool
- Consumer crashed: Check pod logs, restart if needed

### Error Rate High

**Symptoms**: Error rate > 1%

**Investigation Steps**:
1. Breakdown errors by type:
   ```promql
   sum(rate(sykmelding_creation_failed_total[5m])) by (error_type)
   ```
2. Check validation failures:
   ```promql
   sum(rate(sykmelding_verified_total{regula_status!="OK"}[5m]))
   ```
3. Review recent code deployments
4. Check for patterns in error logs

**Common Causes**:
- Bad data from client
- External service returning errors
- New validation rules too strict
- Bug in recent deployment

**Resolution**:
- Bad data: Work with frontend team to fix validation
- External service: Temporary - monitor
- Validation rules: Review with domain experts, adjust if needed
- Bug: Rollback deployment, fix bug

### Latency High

**Symptoms**: p95 latency > SLO (2000ms create, 500ms verify)

**Investigation Steps**:
1. Break down latency by component:
   - Rule validation: `sykmelding_rule_validation_duration_seconds`
   - Database save: `sykmelding_database_save_duration_seconds`
   - Kafka send: `sykmelding_kafka_send_duration_seconds`
2. Check database query performance
3. Check external service response times
4. Look for N+1 query patterns in traces (Tempo)

**Common Causes**:
- Database slow queries / connection pool exhaustion
- External service (PDL/HPR) slow
- Regula rule validation slow (complex rules)
- Kafka broker slow

**Resolution**:
- Database: Optimize queries, add indexes, scale connection pool
- External services: Temporary - monitor
- Rule validation: Profile Regula, optimize rules
- Kafka: Check with NAIS team

### Error Queue

**Symptoms**: `sykmelding_error_queue_size` growing

**Investigation Steps**:
1. Query error repository for patterns:
   ```sql
   SELECT error, COUNT(*) FROM kafka_processing_errors GROUP BY error;
   ```
2. Check error types:
   ```promql
   sum(sykmelding_kafka_processing_error_total) by (exception_type)
   ```
3. Review sample error stack traces

**Common Causes**:
- Systematic data quality issue
- External service consistently failing for certain cases
- Bug in consumer logic

**Resolution**:
- Identify pattern, fix root cause
- If unfixable data: Add to poison pills
- Clear resolved errors from database periodically

### Database Slow

**Symptoms**: Database query latency p95 > 1000ms

**Investigation Steps**:
1. Identify slow operations:
   ```promql
   histogram_quantile(0.95, sum(rate(sykmelding_database_query_duration_seconds_bucket[5m])) by (le, operation))
   ```
2. Check CloudSQL metrics (CPU, connections, IOPS)
3. Review slow query logs
4. Check for missing indexes

**Common Causes**:
- Missing database indexes
- Full table scans on growing tables
- Connection pool exhaustion
- CloudSQL instance under-provisioned

**Resolution**:
- Add indexes for frequently queried columns
- Optimize queries to use indexes
- Increase connection pool size if needed
- Scale CloudSQL instance if under-provisioned

## Runbook Links

For detailed operational procedures, see:
- [Incident Response](https://github.com/navikt/syk-inn-api/blob/main/docs/incident-response.md)
- [Deployment Guide](https://github.com/navikt/syk-inn-api/blob/main/docs/deployment.md)
- [Architecture Overview](https://github.com/navikt/syk-inn-api/blob/main/README.md)

## Contact

- **Team**: syk-inn (Team Sykmelding Innsending)
- **Slack**: `#team-syk-inn`
- **On-call**: PagerDuty rotation

