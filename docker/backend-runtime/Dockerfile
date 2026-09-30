# MVP-01, RUN-QA
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --home-dir /app rbot
COPY target/rbot-1.0.0.jar /app/rbot.jar
USER rbot
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/rbot.jar"]
