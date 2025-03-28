FROM gcr.io/distroless/java21-debian12@sha256:57c651424df553185b259f0b2f9ceca01430bfd69d2879d48870c04c19dd1a3f
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]