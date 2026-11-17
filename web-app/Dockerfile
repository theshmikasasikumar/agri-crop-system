
FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app/web-app

COPY web-app/pom.xml .
COPY web-app/src ./src
COPY web-app/web ./web
COPY database ./database

RUN mvn -DskipTests package

CMD ["mvn", "-Dexec.mainClass=com.agri.web.WebServer", "exec:java"]
