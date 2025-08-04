FROM gcr.io/distroless/java21-debian12@sha256:aa19d82fd66b25cd9c5658c8474ab2bd3af96ef4cf426b1e05d11b17632b31bd
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]