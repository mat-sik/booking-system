# syntax=docker/dockerfile:1.12.0

FROM amazoncorretto:23 AS build-stage

# Install gzip
RUN yum install -y gzip && yum install -y tar

WORKDIR /app

# copy build files
COPY command-service/mvnw .
COPY command-service/pom.xml .
COPY command-service/.mvn .mvn
COPY command-service/src src
COPY commons-models/target/commons-models-1.0-SNAPSHOT.jar commons-models.jar
COPY commons-json/target/commons-json-1.0-SNAPSHOT.jar commons-json.jar
COPY commons-kafka/target/commons-kafka-1.0-SNAPSHOT.jar commons-kafka.jar

# Install local dependencies manually
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw install:install-file -Dfile=./commons-models.jar \
        -DgroupId=com.github.matsik \
        -DartifactId=commons-models \
        -Dversion=1.0-SNAPSHOT \
        -Dpackaging=jar && \
    ./mvnw install:install-file -Dfile=./commons-json.jar \
        -DgroupId=com.github.matsik \
        -DartifactId=commons-json \
        -Dversion=1.0-SNAPSHOT \
        -Dpackaging=jar && \
    ./mvnw install:install-file -Dfile=./commons-kafka.jar \
       -DgroupId=com.github.matsik \
       -DartifactId=commons-kafka \
       -Dversion=1.0-SNAPSHOT \
       -Dpackaging=jar && \
    ./mvnw install -DskipTests

# extract files from jar file
RUN java -Djarmode=layertools -jar target/*.jar extract

# move to release stage
FROM amazoncorretto:23-alpine AS release-stage

WORKDIR /app
# copy files from build-stage
COPY --from=build-stage /app/dependencies .
COPY --from=build-stage /app/snapshot-dependencies .
COPY --from=build-stage /app/spring-boot-loader .
COPY --from=build-stage /app/application .

EXPOSE 8080
CMD ["java", "org.springframework.boot.loader.launch.JarLauncher"]