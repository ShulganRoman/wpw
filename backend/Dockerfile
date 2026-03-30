FROM amazoncorretto:17-alpine AS builder

WORKDIR /app

RUN apk add --no-cache bash

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw clean package -DskipTests -q

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
