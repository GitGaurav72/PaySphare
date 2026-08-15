# Build the Angular app first. Its production API URL is the same-origin /api.
FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend

COPY src/paySphere-frontend/package.json src/paySphere-frontend/package-lock.json ./
RUN npm ci
COPY src/paySphere-frontend/ ./
RUN npm run build

# Package Spring Boot with the Angular build in src/main/resources/static.
FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/backend

COPY src/PaySphere/pom.xml ./
RUN mvn --batch-mode dependency:go-offline
COPY src/PaySphere/ ./
COPY --from=frontend-build /workspace/frontend/dist/paysphere-frontend/browser/ src/main/resources/static/
RUN mvn --batch-mode package -DskipTests

# The final image contains only the Java runtime and the packaged application.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/PaySphere-0.0.1-SNAPSHOT.war app.war

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.war"]
