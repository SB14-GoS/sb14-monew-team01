FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --chown=app:app build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
