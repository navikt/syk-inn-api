# syk-inn-api

## Technologies used

* Kotlin
* Spring boot
* Gradle

### Prerequisites

(Use mise? `mise i` to install the required prerequisites)

- Java 21

You will also need docker installed for running the application locally, and running tests with testcontainers.

### Building the application

To build locally and run the integration tests you can simply run

``` bash
./gradlew clean build
```

This will run tests as well.

#### Colima sidenote

Ensure Testcontainers has access to Docker by adding this to your configuration
```
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export DOCKER_HOST="unix://${HOME}/.colima/docker.sock"
export TESTCONTAINERS_HOST_OVERRIDE=$(colima ls -j | jq -r '.address')
```

## Run this application locally

external services are mocked so we are not calling pdl, btsys etc-

``` bash
docker compose up -d 
```

or

``` bash
podman-compose up -d
```

Run the application normally from IntelliJ, remember to set the profile `local` in your run configuration.

Or run the application using Gradle:

``` bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Inspecting topics

Kafka will run in docker. To query the local Kafka instance you can use the following command to check for
content in the topic::

``` bash
kcat -b localhost:9092 -t tsm.sykmeldinger-input -C -o beginning
```

## Contact

This project is maintained by [navikt/tsm](CODEOWNERS)

Questions and/or feature requests?
Please create an [issue](https://github.com/navikt/syk-inn-api/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
