FROM eclipse-temurin:11-jdk-alpine
WORKDIR /app
COPY sentinel-dashboard.jar sentinel-dashboard.jar
EXPOSE 8090
ENTRYPOINT ["java", "-Xmx256m", "-Xms256m","-Dserver.port=8090", "-Dcsp.sentinel.dashboard.server=localhost:8090", "-Dproject.name=sentinel-dashboard", "-jar", "sentinel-dashboard.jar"]