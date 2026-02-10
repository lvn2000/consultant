# Multi-stage build for optimal image size

# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Install SBT using modern keyring approach (not deprecated apt-key)
RUN apt-get update && apt-get install -y curl gnupg2 && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" | gpg --dearmor > /usr/share/keyrings/sbt-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/sbt-archive-keyring.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    apt-get update && apt-get install -y sbt && \
    rm -rf /var/lib/apt/lists/*

# Copy build configuration files
COPY build.sbt .
COPY project project/

# Cache dependencies
RUN sbt update

# Copy source code
COPY core core/
COPY data data/
COPY infrastructure infrastructure/
COPY api api/

# Build the project
RUN sbt api/assembly

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create service user for security
RUN groupadd -r consultant && useradd -r -g consultant consultant

# Copy the assembled JAR
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

# Application port
EXPOSE 8090

# Run with unprivileged user
USER consultant

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
