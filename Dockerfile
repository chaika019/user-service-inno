FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY target/user-service-chaika-inno-0.0.1.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]