ARG VERSION_TEMURIN
FROM eclipse-temurin:$VERSION_TEMURIN
LABEL maintener=EXEMPLE_TEST
COPY target/*.jar exemple-gateway-integration-resource.jar
ENTRYPOINT ["java","-jar","exemple-gateway-integration-resource.jar"]