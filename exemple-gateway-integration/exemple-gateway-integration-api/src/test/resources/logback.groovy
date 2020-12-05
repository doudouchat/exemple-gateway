import java.nio.charset.Charset
import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN

import ch.qos.logback.classic.encoder.PatternLayoutEncoder

scan("30 seconds")

def USER_HOME = "${project.build.directory}"
def LOGS_FOLDER = "${USER_HOME}/.logs"
def LOG_ARCHIVE = "${LOGS_FOLDER}/archive"
def LOGS_FILENAME = "exemple_gateway"

appender("console", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d %-5p: %C - %m%n"
        charset =  Charset.forName("UTF-8")
    }
}

appender("archive", RollingFileAppender) {
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "${LOG_ARCHIVE}/${LOGS_FILENAME}.%d{yyyy-MM-dd}.log"
        maxHistory = 2
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%d %-5p: %C - %m%n"
        charset =  Charset.forName("UTF-8")
    }
}

logger("org.springframework.boot", INFO)
logger("org.springframework.cloud", DEBUG)
logger("com.exemple.gateway", DEBUG)
logger("com.exemple.service", DEBUG)
logger("org.springframework.web.filter.CommonsRequestLoggingFilter", DEBUG)

root(WARN, ["console", "archive"])