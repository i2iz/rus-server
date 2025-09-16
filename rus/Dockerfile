# Stage 1
FROM openjdk:21-jdk-slim AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies

COPY src src

RUN ./gradlew bootJar

# Stage 2
FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

# RUN
ENTRYPOINT [ "java", "-jar", "app.jar" ]

EXPOSE 8080
