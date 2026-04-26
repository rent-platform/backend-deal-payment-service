FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY service/build/libs/*.jar app.jar
COPY database/migrations /app/database/migrations

ENTRYPOINT ["java", "-jar", "app.jar"]