# --- Build stage ---
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace
COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY config ./config
COPY src ./src
RUN chmod +x ./gradlew && ./gradlew --no-daemon shadowJar -x checkstyleMain -x spotlessCheck

# --- Runtime stage ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# вшиваем сертификат от минцифры для работы гигачат
COPY russian_trusted_root_ca.cer /tmp/mintsifra.cer
RUN keytool -importcert -trustcacerts -file /tmp/mintsifra.cer -alias mintsifra -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt \
    && rm /tmp/mintsifra.cer

COPY --from=builder /workspace/build/libs/book-tracker-advisor-bot.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]