FROM gcr.io/distroless/java21-debian12@sha256:d2d4515f1062fac83c307260a14b523fe6027d0ce22e3b77abfc8bef874b5497
WORKDIR /app
COPY build/libs/*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]