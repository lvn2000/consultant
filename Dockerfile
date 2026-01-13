# Multi-stage build для оптимального размера образа

# Stage 1: Build
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.9.7_3.3.1 AS builder

WORKDIR /app

# Копируем файлы сборки
COPY build.sbt .
COPY project project/

# Кешируем зависимости
RUN sbt update

# Копируем исходный код
COPY core core/
COPY data data/
COPY infrastructure infrastructure/
COPY api api/

# Собираем проект
RUN sbt api/assembly

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Создаем пользователя для безопасности
RUN groupadd -r consultant && useradd -r -g consultant consultant

# Копируем собранный jar
COPY --from=builder /app/api/target/scala-3.3.1/*-assembly*.jar app.jar

# Настраиваем JVM для контейнера
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Порт приложения
EXPOSE 8080

# Запускаем от непривилегированного пользователя
USER consultant

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
