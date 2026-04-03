FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG APP_VERSION=dev
ENV VITE_APP_VERSION=${APP_VERSION}

RUN npm run build


FROM alpine:latest

COPY --from=builder /app/dist /app/dist

ENTRYPOINT ["sh", "-c", "cp -r /app/dist/. /static && echo 'Static files ready'"]
