# Stage 1: Build with Maven
# amazoncorretto:17-alpine has multi-arch support (ARM64 + AMD64)
FROM amazoncorretto:17-alpine AS builder

WORKDIR /app

# Install bash (required by mvnw wrapper)
RUN apk add --no-cache bash

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw

# Download dependencies separately for layer caching
RUN ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Run with minimal JRE
FROM amazoncorretto:17-alpine

WORKDIR /app

RUN apk add --no-cache libwebp-tools

RUN addgroup -S pim && adduser -S pim -G pim

COPY --from=builder /app/target/pim-0.0.1-SNAPSHOT.jar app.jar
RUN chown pim:pim app.jar

USER pim

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
