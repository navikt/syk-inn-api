FROM gcr.io/distroless/java21-debian12@sha256:7c05bf8a64ff1a70a16083e9bdd35b463aa0d014c2fc782d31d13ea7a61de633
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]