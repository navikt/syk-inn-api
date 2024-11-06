# syk-inn-apo

## Technologies used
* Kotlin
* Spring boot
* Gradle

### Prerequisites
Make sure you have the Java JDK 21 installed
You can check which version you have installed using this command:
``` bash
java -version
```

Make sure you have the Docker installed
You can check which version you have installed using this command:
``` bash
docker --version
```

### Building the application
To build locally and run the integration tests you can simply run
``` bash
./gradlew bootJar
```
or on windows
`gradlew.bat bootJar`

### Running the application locally
#### With bootRun
run this command
``` bash
./gradlew bootRun
```
or on windows
`gradlew.bat bootRun`

#### Docker
##### Creating a docker image
Creating a docker image should be as simple as
``` bash
docker build -t syk-inn-api .
```
> [!NOTE]  
> Remember to build the application before,you create a docker image, see [Building the application](#building-the-application)


#### Running a docker image
``` bash
docker run -d --rm -it -p 8080:8080 syk-inn-api
```

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