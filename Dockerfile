ARG VERSION_OPENJDK
FROM openjdk:$VERSION_OPENJDK
LABEL maintener=EXEMPLE
COPY exemple-gateway-server/target/*.jar exemple-gateway-server.jar
ENTRYPOINT ["java","-jar","exemple-gateway-server.jar"]