# =============================================================================
# 後端 Spring Boot Dockerfile (多階段建置)
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B || true

COPY src ./src
RUN mvn clean package -DskipTests

# 執行階段
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 安全防護：建立非 root 執行使用者
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/pos-analytics-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
