FROM gcr.io/distroless/java21-debian12@sha256:6826e286459c15a54bc0bb746cf19a12cefc5d35923ebe49ef88008858f3ac00
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]