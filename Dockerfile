FROM gcr.io/distroless/java21-debian12@sha256:379b83f022ff660e09976b6680f05dbd4a4e751dc747a122aebd348402f11e4b
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]