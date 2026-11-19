FROM maven:3.9.14-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY pom.xml ./

RUN mvn -B -ntp -DskipTests dependency:go-offline

COPY src/ src/

RUN mvn -B -ntp -DskipTests package


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/target/legal-cases-backend.jar /app/app.jar
COPY --from=builder /workspace/target/legal-cases-backend-migrations.jar /app/migrations.jar

RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
