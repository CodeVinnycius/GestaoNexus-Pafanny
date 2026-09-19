# ── Build ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Camada de dependências separada para cachear entre builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ── Runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Railway injeta a variavel PORT em runtime; ver application.properties
ENTRYPOINT ["java", "-jar", "app.jar"]
