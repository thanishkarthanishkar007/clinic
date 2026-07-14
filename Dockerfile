FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY . .

RUN apk add --no-cache maven

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java","-jar","target/devs-clinic-backend-1.0.0.jar"]