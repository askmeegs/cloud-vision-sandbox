# Source: https://spring.io/guides/topicals/spring-boot-docker/
# https://www.docker.com/blog/9-tips-for-containerizing-your-spring-boot-code/
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /opt/app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY ./src ./src
RUN ./mvnw clean install
 
FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/app
EXPOSE 8080
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar
COPY sa-key.json /opt/app/sa-key.json
COPY src/main/resources/static/wakeupcat.jpg /opt/app/wakeupcat.jpg
ENV GOOGLE_APPLICATION_CREDENTIALS=/opt/app/sa-key.json
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar" ]