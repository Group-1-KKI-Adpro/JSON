FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon
RUN JAR_FILE=$(ls build/libs/*.jar | grep -v plain | head -n 1) && cp "$JAR_FILE" app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/app.jar app.jar

EXPOSE 8080

CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]