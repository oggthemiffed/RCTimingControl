# syntax=docker/dockerfile:1
FROM node:20-alpine AS builder
WORKDIR /build
COPY frontend/package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend/ .
RUN npm run build

FROM nginx:stable-alpine AS runtime
COPY --from=builder /build/dist/ /usr/share/nginx/html/
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
