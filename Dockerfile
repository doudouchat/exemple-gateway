ARG VERSION_OPENJDK=latest
FROM openjdk:$VERSION_OPENJDK
LABEL maintener=EXEMPLE
COPY exemple-gateway-launcher/target/*.jar exemple-gateway-launcher.jar
ENTRYPOINT ["java","-jar","exemple-gateway-launcher.jar"]