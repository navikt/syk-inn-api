FROM gcr.io/distroless/java21-debian12@sha256:bcc3b8fc7f5abea2efaa3a81e7ddffc2424e9c1f053561a327008d9aee29dda8
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]