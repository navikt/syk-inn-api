FROM gcr.io/distroless/java21-debian12@sha256:69c841c01c293c13e22ccd3deac56a3e24fbb3862517b74eed3e4e15c36bce64
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]