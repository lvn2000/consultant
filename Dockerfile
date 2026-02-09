# Multi-stage build для оптимального размера образа

# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Install SBT
RUN apt-get update && apt-get install -y curl gnupg2 && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" | apt-key add && \
    apt-get update && apt-get install -y sbt && \
    rm -rf /var/lib/apt/lists/*

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
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Создаем пользователя для безопасности
RUN groupadd -r consultant && useradd -r -g consultant consultant

    # Копируем собранный jar
COPY --from=builder /app/api/target/scala-3.4.2/*-assembly*.jar app.jar

# Настраиваем JVM для контейнера
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8090/health || exit 1

# Порт приложения
EXPOSE 8090

# Запускаем от непривилегированного пользователя
USER consultant

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
