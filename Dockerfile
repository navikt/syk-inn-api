FROM gcr.io/distroless/java21-debian12@sha256:a4f6bbb87e71a7b66779c1df54aaed68623dbd861e957972ee9f2b5c3e050075
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]