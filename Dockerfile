FROM gcr.io/distroless/java21-debian12@sha256:903d5ad227a4afff8a207cd25c580ed059cc4006bb390eae65fb0361fc9724c3
WORKDIR /app
COPY build/libs/*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]