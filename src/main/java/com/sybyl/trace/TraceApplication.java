package com.sybyl.trace;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class TraceApplication {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(TraceApplication.class);

        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {

            String logDir = event.getEnvironment().getProperty("LOG_DIR");

            if (logDir == null || logDir.isBlank()) {
                logDir = "D:/trace_log";
            }

            File dir = new File(logDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    log.info("Log directory created at: {}", dir.getAbsolutePath());
                } else {
                    log.warn("Failed to create log directory at: {}", dir.getAbsolutePath());
                }
            } else {
                log.info("Using existing log directory: {}", dir.getAbsolutePath());
            }
        });

        log.info("TraceApplication starting...");
        app.run(args);
        log.info("TraceApplication started successfully.");
    }
}
