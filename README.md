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

### Running the application locally
#### With bootRun
> [!NOTE]  
> Remember to run the external services the application needs to be able to run, see [Running the application and the needed external services](#running-the-application-and-the-needed-external-services)

run this command
``` bash
SECURELOGS_DIR=./ AZURE_APP_WELL_KNOWN_URL='http://localhost:${mock-oauth2-server.port}/azuread/.well-known/openid-configuration' AZURE_APP_CLIENT_ID='syk-inn-api-client-id' AZURE_OPENID_CONFIG_TOKEN_ENDPOINT='http://localhost:${mock-oauth2-server.port}/azuread/token' SYFOHELSENETTPROXY_SCOPE=syfohelsenettproxyscope AZURE_APP_CLIENT_SECRET=secretzz MOCK_OAUTH2_SERVER_PORT=6969 KAFKA_BROKERS='localhost:9092' KAFKA_TRUSTSTORE_PATH='' KAFKA_CREDSTORE_PASSWORD='' KAFKA_SECURITY_PROTOCOL='PLAINTEXT' KAFKA_KEYSTORE_PATH='' SYFOSMREGLER_SCOPE=syfosmreglerscope SYFOSMREGISTER_SCOPE=syfosmregisterscope AZURE_OPENID_CONFIG_ISSUER='http://localhost:${mock-oauth2-server.port}/azuread' ./gradlew bootRun
```
or on windows
`SECURELOGS_DIR=./ AZURE_APP_WELL_KNOWN_URL='http://localhost:${mock-oauth2-server.port}/azuread/.well-known/openid-configuration' AZURE_APP_CLIENT_ID='syk-inn-api-client-id' AZURE_OPENID_CONFIG_TOKEN_ENDPOINT='http://localhost:${mock-oauth2-server.port}/azuread/token' SYFOHELSENETTPROXY_SCOPE=syfohelsenettproxyscope AZURE_APP_CLIENT_SECRET=secretzz MOCK_OAUTH2_SERVER_PORT=6969 KAFKA_BROKERS='localhost:9092' KAFKA_TRUSTSTORE_PATH='' KAFKA_CREDSTORE_PASSWORD='' KAFKA_SECURITY_PROTOCOL='PLAINTEXT' KAFKA_KEYSTORE_PATH='' SYFOSMREGLER_SCOPE=syfosmreglerscope SYFOSMREGISTER_SCOPE=syfosmregisterscope AZURE_OPENID_CONFIG_ISSUER='http://localhost:${mock-oauth2-server.port}/azuread' gradlew.bat bootRun`

#### With docker
##### Creating a docker image
Creating a docker image should be as simple as
``` bash
docker build -t syk-inn-api .
```
> [!NOTE]  
> Remember to build the application before,you create a docker image, see [Building the application](#building-the-application)

### Running the application and the needed external services
``` bash
docker compose up
```

### Run a local postgres database
``` bash
docker run --name my-postgres \
  -e POSTGRES_DB=sykinnapi \
  -e POSTGRES_USER=myuser \
  -e POSTGRES_PASSWORD=mypassword \
  -p 5432:5432 \
  -d postgres:16
```

#### Api doc
Locally
http://localhost:8080/v3/api-docs
or on
http://syk-inn-api.intern.dev.nav.no:8080/v3/api-docs

### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

``` bash
./gradlew wrapper --gradle-version $gradleVersjon
```

### Contact
This project is maintained by [navikt/tsm](CODEOWNERS)

Questions and/or feature requests?
Please create an [issue](https://github.com/navikt/syk-inn-api/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)