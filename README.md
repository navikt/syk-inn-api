# syk-inn-api

## Technologies used
* Kotlin
* Spring boot
* Gradle

### Prerequisites
#### Java
Make sure you have the Java JDK 21 installed
You can check which version you have installed using this command:
``` bash
java -version
```
#### Docker
Make sure you have the Docker installed
You can check which version you have installed using this command:
``` bash
docker --version
```
#### Docker compose
Make sure you have the Docker-compose installed
You can check which version you have installed using this command:
``` bash
docker-compose --version
```

Docker compose will deploy Kafka. To query the local Kafka instance you can use the following command to check for content in the topic::
``` bash
 kcat -b localhost:9092 -t tsm.sykmeldinger-input -C -o beginning
```

### Building the application
To build locally and run the integration tests you can simply run
``` bash
./gradlew clean build
```
or on windows
`gradlew.bat clean build`


---
> new stuff under here this is the stuff you should read
# Run this application locally

external services are mocked so we are not calling pdl, btsys etc- 

``` bash
docker compose up -d
or 
podman-compose up -d 
```

## Initiate the database table 
currently flyway is not enabled so before you send a create sykmelding you HAVE to create the sykmelding table. Start the db query console in your IDe and run this: 

``` sql
CREATE TABLE sykmelding (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            sykmelding_id TEXT NOT NULL,
                            pasient_fnr TEXT NOT NULL,
                            sykmelder_hpr TEXT NOT NULL,
                            sykmelding JSONB NOT NULL,
                            legekontor_orgnr TEXT NOT NULL,
                            validert_ok BOOLEAN NOT NULL DEFAULT FALSE
);
```


### Contact
This project is maintained by [navikt/tsm](CODEOWNERS)

Questions and/or feature requests?
Please create an [issue](https://github.com/navikt/syk-inn-api/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
