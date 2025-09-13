FROM amazoncorretto:24 AS build-stage

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY commons-models/pom.xml commons-models/
COPY commons-json/pom.xml commons-json/
COPY commons-kafka/pom.xml commons-kafka/
COPY command-service/pom.xml command-service/
COPY query-service/pom.xml query-service/
COPY booking-service/pom.xml booking-service/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B dependency:go-offline -DskipTests

COPY commons-models ./commons-models
COPY commons-json ./commons-json
COPY commons-kafka ./commons-kafka
COPY command-service ./command-service
COPY query-service ./query-service
COPY booking-service ./booking-service

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests -pl query-service -am

RUN java -Djarmode=layertools -jar query-service/target/*.jar extract

FROM amazoncorretto:24-alpine AS release-stage

WORKDIR /app

COPY --from=build-stage /app/dependencies .
COPY --from=build-stage /app/snapshot-dependencies .
COPY --from=build-stage /app/spring-boot-loader .
COPY --from=build-stage /app/application .

EXPOSE 8080
CMD ["java", "org.springframework.boot.loader.launch.JarLauncher"]