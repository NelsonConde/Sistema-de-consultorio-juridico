FROM maven:3.9.14-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY pom.xml ./

# Use the normal Maven Central hostname first; use its official alternate only when resolution fails.
RUN if mvn -B -ntp -U -DskipTests dependency:go-offline; then \
      :; \
    else \
      cp .mvn/settings-central-fallback.xml /tmp/maven-settings.xml; \
      mvn -B -ntp -U -s /tmp/maven-settings.xml -DskipTests dependency:go-offline; \
    fi

COPY src/ src/

RUN if [ -f /tmp/maven-settings.xml ]; then \
      mvn -B -ntp -U -s /tmp/maven-settings.xml -DskipTests package; \
    else \
      mvn -B -ntp -U -DskipTests package; \
    fi


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/target/legal-cases-backend.jar /app/app.jar
COPY --from=builder /workspace/target/legal-cases-backend-migrations.jar /app/migrations.jar

RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
