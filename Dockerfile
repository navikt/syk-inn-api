FROM gcr.io/distroless/java21-debian12@sha256:75bff39aa6eaaa759db7b42c68ec63b444ce981d65b8682264ba29724411e0bd
WORKDIR /app
COPY build/libs/*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]