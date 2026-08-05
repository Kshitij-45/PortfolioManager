# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy build files first to maximize layer cache reuse.
COPY .mvn/ .mvn/
COPY mvnw mvnw
COPY pom.xml pom.xml
RUN chmod +x mvnw

# Copy source and frontend assets.
COPY src/ src/
COPY frontend/ frontend/

# Build executable artifact.
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Copy the built Spring Boot WAR.
COPY --from=builder /app/target/*.war /app/app.war

# Keep frontend folder available for spring.web.resources.static-locations=file:./frontend/
COPY frontend/ /app/frontend/

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.war"]
