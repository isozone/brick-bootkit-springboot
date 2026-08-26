FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/quickstart-worker-1.0.0.jar app.jar
EXPOSE 8081 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
