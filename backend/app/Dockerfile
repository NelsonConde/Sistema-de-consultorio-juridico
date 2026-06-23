FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -q -DskipTests package


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/target/*.jar /app/app.jar

RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
