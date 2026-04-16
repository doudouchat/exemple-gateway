ARG VERSION_TEMURIN=latest
FROM eclipse-temurin:$VERSION_TEMURIN
LABEL maintener=EXEMPLE
RUN apt-get -y update; apt-get -y install curl
COPY exemple-gateway-launcher/target/*.jar exemple-gateway-launcher.jar
ENTRYPOINT ["java","-jar","exemple-gateway-launcher.jar"]