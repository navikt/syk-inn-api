FROM gcr.io/distroless/java21-debian12@sha256:4a57d2f519c89a442dc05312750e31e7736ae004c2bb827365d8565792c0f37a
WORKDIR /app
COPY build/libs/*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]