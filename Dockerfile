FROM gcr.io/distroless/java21-debian12@sha256:c298bfc8c8b1aa3d7b03480dcf52001a90d66d966f6a8d8997ae837d3982be3f
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]