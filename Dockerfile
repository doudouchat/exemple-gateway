FROM openjdk:8-jdk-buster
LABEL maintener=EXEMPLE
COPY exemple-gateway-server/target/*.jar exemple-gateway-server.jar
ENTRYPOINT ["java","-jar","exemple-gateway-server.jar"]