FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S retailhub && adduser -S retailhub -G retailhub
WORKDIR /app
COPY --from=build /workspace/target/retailhub-*.jar app.jar
USER retailhub
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

