FROM gcr.io/distroless/java21-debian12@sha256:7c9a9a362eadadb308d29b9c7fec2b39e5d5aa21d58837176a2cca50bdd06609
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]