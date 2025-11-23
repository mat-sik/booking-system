FROM amazoncorretto:25 AS build-stage

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY commons-models/pom.xml commons-models/
COPY commons-kafka/pom.xml commons-kafka/
COPY commons-grpc-query-service/pom.xml commons-grpc-query-service/
COPY command-service/pom.xml command-service/
COPY query-service/pom.xml query-service/
COPY booking-service/pom.xml booking-service/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B dependency:go-offline -DskipTests

COPY commons-models ./commons-models
COPY commons-kafka ./commons-kafka
COPY commons-grpc-query-service ./commons-grpc-query-service
COPY command-service ./command-service
COPY query-service ./query-service
COPY booking-service ./booking-service

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests -pl command-service -am

RUN java -Djarmode=layertools -jar command-service/target/*.jar extract

FROM amazoncorretto:25-alpine AS release-stage

WORKDIR /app

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.22.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

COPY --from=build-stage /app/dependencies .
COPY --from=build-stage /app/snapshot-dependencies .
COPY --from=build-stage /app/spring-boot-loader .
COPY --from=build-stage /app/application .

CMD ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "org.springframework.boot.loader.launch.JarLauncher"]