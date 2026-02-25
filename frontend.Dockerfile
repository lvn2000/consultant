# Multi-stage build for Nuxt.js applications (admin, client, specialist)
# Usage: docker build --build-arg APP_DIR=admin-app -f frontend.Dockerfile .
# Optimized for production with minimal image size

ARG APP_DIR=admin-app

# Stage 1: Base
FROM node:20-alpine AS base
WORKDIR /app

# Stage 2: Dependencies
FROM base AS deps
ARG APP_DIR
COPY ${APP_DIR}/package.json ${APP_DIR}/package-lock.json ./
RUN npm ci

# Stage 3: Build
FROM base AS builder
ARG APP_DIR
COPY --from=deps /app/node_modules ./node_modules

# Copy application source first
COPY ${APP_DIR} .

# Remove symlink if exists (will be replaced with real directory)
RUN rm -rf i18n || true

# Copy i18n files (shared across all frontend apps)
COPY i18n ./i18n

# Enable corepack for pnpm/yarn if needed
RUN corepack enable || true

# Build the application
RUN npm run build

# Stage 4: Runner (production image)
FROM node:20-alpine AS runner
WORKDIR /app

ARG APP_DIR
ENV NODE_ENV=production
ENV NUXT_HOST=0.0.0.0
ENV NUXT_PORT=3000

# Create non-root user for security
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nuxtuser

# Copy built application from builder stage
COPY --from=builder --chown=nuxtuser:nodejs /app/.output ./.output

USER nuxtuser

EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3000/ || exit 1

# Start the application
CMD ["node", ".output/server/index.mjs"]
