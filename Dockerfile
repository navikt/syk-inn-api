FROM gcr.io/distroless/java21-debian12@sha256:2bda49bc3f1dac94d4b8b2133545ab17f7be6c4216be8cea589d2d52660da308
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]