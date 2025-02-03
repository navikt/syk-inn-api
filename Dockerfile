FROM gcr.io/distroless/java21-debian12@sha256:70e8a4991b6e37cb1eb8eac3b717ed0d68407d1150cf30235d50cd33b2c44f7e
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]