# syk-inn-api

## Technology Stack

- **Language**: Kotlin 2.2.0
- **Framework**: Spring Boot 3.5.4
- **Build Tool**: Gradle with Kotlin DSL
- **Database**: PostgreSQL 16 with Flyway migrations
- **Message Broker**: Apache Kafka
- **Security**: OAuth2 with JWT (Azure AD)
- **PDF Generation**: OpenHTMLToPDF with PDF/A validation
- **JVM**: Java 21

## Key Dependencies

- **Regulus**: Business rule validation engine (`no.nav.tsm.regulus:regula`)
- **Sykmelding Input**: Kafka message definitions (`no.nav.tsm.sykmelding:input`)
- **Arrow**: Functional programming with Either types for error handling
- **Spring Data JPA**: Database persistence
- **Spring Kafka**: Kafka integration
- **Testcontainers**: Integration testing

---

## High-Level Architecture

```mermaid
graph TB
    Client[External Client/UI]
    API[Syk-Inn-API<br/>Spring Boot Application]
    
    subgraph External Services
        PDL[PDL<br/>Person Data]
        HPR[Helsenett Proxy<br/>Healthcare Professional Registry]
        BTSYS[BTSYS<br/>Suspension System]
    end
    
    subgraph Data Layer
        DB[(PostgreSQL<br/>Database)]
        Kafka[Kafka Topic<br/>tsm.sykmeldinger-input]
    end
    
    Client -->|REST API| API
    API -->|Get Person Info| PDL
    API -->|Get Sykmelder Info| HPR
    API -->|Check Suspension| BTSYS
    API -->|Store| DB
    API -->|Publish| Kafka
    
    style API fill:#4A90E2,color:#fff
    style DB fill:#2ECC71,color:#fff
    style Kafka fill:#E74C3C,color:#fff
```

### Prerequisites

(Use mise? `mise i` to install the required prerequisites)

- Java 21

You will also need docker installed for running the application locally, and running tests with testcontainers.

### Building the application

1. Start infrastructure: `docker-compose up -d`
2. Run:
   1. Run in IntelliJ - set profiles local and def-kafka
   2. Run with local profile: `./gradlew bootRun --args='--spring.profiles.active=local,dev-kafka'`
4. External services are mocked (no real PDL, BTSYS, HPR calls)

This will run tests as well.

### Inspecting topics

Kafka will run in docker. To query the local Kafka instance you can use the following command to check for
content in the topic::

``` bash
kcat -b localhost:9092 -t tsm.sykmeldinger-input -C -o beginning
```

#### Colima sidenote

Ensure Testcontainers has access to Docker by adding this to your configuration
```
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export DOCKER_HOST="unix://${HOME}/.colima/docker.sock"
export TESTCONTAINERS_HOST_OVERRIDE=$(colima ls -j | jq -r '.address')
```

# Architecture

## Application Flow - Create Sykmelding

```mermaid
sequenceDiagram
    participant Client
    participant Controller as SykmeldingController
    participant Service as SykmeldingService
    participant Rules as RuleService
    participant Person as PersonService
    participant Sykmelder as SykmelderService
    participant Persistence as PersistenceService
    participant Kafka as KafkaProducer
    participant DB as Database
    
    Client->>Controller: POST /api/sykmelding
    Controller->>Service: createSykmelding(payload)
    
    Service->>Service: Generate UUID & timestamp
    
    par Fetch External Resources
        Service->>Person: getPersonByIdent()
        Person->>PDL: Query person data
        PDL-->>Person: Return person info
        Person-->>Service: Person object
    and
        Service->>Sykmelder: sykmelderMedSuspensjon()
        Sykmelder->>HPR: Get sykmelder by HPR
        HPR-->>Sykmelder: Sykmelder info
        Sykmelder->>BTSYS: Check suspension
        BTSYS-->>Sykmelder: Suspension status
        Sykmelder-->>Service: Sykmelder object
    end
    
    Service->>Rules: validateRules()
    Rules->>Rules: Execute Regula rules
    Rules-->>Service: ValidationResult
    
    Service->>Persistence: saveSykmeldingPayload()
    Persistence->>DB: INSERT sykmelding
    DB-->>Persistence: Saved entity
    Persistence-->>Service: SykmeldingDocument
    
    Service->>Kafka: send(sykmeldingRecord)
    Kafka->>Kafka: Publish to topic
    
    Service-->>Controller: Either<Error, SykmeldingDocument>
    Controller-->>Client: 201 Created + SykmeldingDocument
```
---

## Rule Validation Flow

```mermaid
graph TB
    Start[Receive Sykmelding Payload]
    Start --> Gather[Gather Required Data]
    
    Gather --> Person[Fetch Person<br/>from PDL]
    Gather --> Sykmelder[Fetch Sykmelder<br/>from HPR/BTSYS]
    
    Person --> Build[Build Regula Payload]
    Sykmelder --> Build
    
    Build --> Execute[Execute Regula Rules]
    
    Execute --> Check{Validation<br/>Result?}
    
    Check -->|OK| Success[Status: OK<br/>Save & Publish]
    Check -->|MANUAL_PROCESSING| Manual[Status: MANUAL<br/>Save & Publish]
    Check -->|INVALID| Invalid[Status: INVALID<br/>Save & Publish]
    
    Success --> Store[(Store in DB)]
    Manual --> Store
    Invalid --> Store
    
    Store --> Kafka[Publish to Kafka]
    Kafka --> End[Return Response]
    
    style Success fill:#2ECC71,color:#fff
    style Manual fill:#F39C12,color:#fff
    style Invalid fill:#E74C3C,color:#fff
```

## Error Handling Strategy

The application uses **Arrow's Either type** for functional error handling:

```kotlin
Either<Error, Success>
```

**Error Types**:
- `SykmeldingCreationErrors.PersonDoesNotExist`: Patient not found in PDL
- `SykmeldingCreationErrors.PersistenceError`: Database save failed
- `SykmeldingCreationErrors.ResourceError`: External service call failed

**Error Flow**:
```mermaid
graph LR
    Service[Service Layer]
    Success{Result?}
    Left[Left: Error]
    Right[Right: Success]
    Controller[Controller]
    
    Service --> Success
    Success -->|Error| Left
    Success -->|Success| Right
    
    Left -->|Map to HTTP| Controller
    Right -->|200/201| Controller
    
    style Left fill:#E74C3C,color:#fff
    style Right fill:#2ECC71,color:#fff
```

## Contact

This project is maintained by [navikt/tsm](CODEOWNERS)

Questions and/or feature requests?
Please create an [issue](https://github.com/navikt/syk-inn-api/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
