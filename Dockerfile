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

# install python 
RUN apt-get update && apt-get install -y python3 python3-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# install gcloud  CLI latest
RUN apt-get update && apt-get install -y curl gnupg && \
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
    curl -sSL https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg  add - && \
    apt-get update && apt-get install -y google-cloud-sdk && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*


EXPOSE 8080
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar
# COPY sa-key.json /opt/app/sa-key.json
# COPY src/main/resources/static/wakeupcat.jpg /opt/app/wakeupcat.jpg
# ENV GOOGLE_APPLICATION_CREDENTIALS=/opt/app/sa-key.json
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar" ]