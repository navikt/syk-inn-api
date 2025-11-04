FROM gcr.io/distroless/java21-debian12@sha256:1fcdb697531e4e1d5acf7e1230a8f007d683321c71f6e4f4063603ecb1502849
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]