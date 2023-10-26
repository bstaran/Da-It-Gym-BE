FROM eclipse-temurin:17-jdk AS build

WORKDIR /usr/src/app
COPY ./ ./
RUN chmod +x ./gradlew
RUN ./gradlew -Dhttp.proxyHost=krmp-proxy.9rum.cc -Dhttp.proxyPort=3128 -Dhttps.proxyHost=krmp-proxy.9rum.cc -Dhttps.proxyPort=3128 clean build --refresh-dependencies

FROM eclipse-temurin:17-jdk

COPY --from=build /usr/src/app/build/libs/da-it-gym-0.0.1-SNAPSHOT.jar /app/da-it-gym-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "/app/da-it-gym-0.0.1-SNAPSHOT.jar"]