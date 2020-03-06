FROM tomcat:9.0.31-jdk8-openjdk
LABEL maintener=EXEMPLE
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY exemple-gateway-server/target/*.war /usr/local/tomcat/webapps/ROOT.war
COPY exemple-gateway-server/src/main/conf/context.xml /usr/local/tomcat/conf/context.xml
COPY exemple-gateway-server/src/main/conf/setenv.sh /usr/local/tomcat/bin/setenv.sh
CMD ["catalina.sh", "run"]