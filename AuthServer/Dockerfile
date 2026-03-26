# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/auth-server-1.0.0.jar auth-server.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "auth-server.jar"]
