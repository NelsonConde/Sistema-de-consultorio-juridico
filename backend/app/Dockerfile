FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -q -DskipTests package


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/target/legal-cases-backend.jar /app/app.jar
COPY --from=builder /workspace/target/legal-cases-backend-migrations.jar /app/migrations.jar

RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
