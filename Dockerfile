FROM gcr.io/distroless/java21-debian12@sha256:41fe95b4bc22b549968d286fe98d242695c6d3b2effb31bb43a4b82b8eed6c19
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]