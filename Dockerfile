# Stage 1: Build the application (Using Temurin based Maven)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application (Using Temurin Runtime)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# Stage 1 se bani hui jar file ko copy karna
COPY --from=build /app/target/*.jar app.jar

# Uploads directory create karna taaki images save ho sakein
RUN mkdir -p uploads/group-avatars

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]