// src/main/java/com/sybyl/trace/Application.java
package com.sybyl.trace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;

@SpringBootApplication
public class TraceApplication {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(TraceApplication.class);

    // EARLY listener to create log directory before Logback starts
    app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
      // We will read the dir from our logback-spring.xml property LOG_DIR
      String logDir = event.getEnvironment().getProperty("LOG_DIR"); // optional override from env/properties
      if (logDir == null || logDir.isBlank()) {
        // fallback if not provided by env/properties; keep in sync with logback-spring.xml
        logDir = "D:/trace_log";
      }
      File dir = new File(logDir);
      if (!dir.exists()) {
        boolean created = dir.mkdirs();
        System.out.println((created ? "✅" : "⚠️") + " Log directory: " + dir.getAbsolutePath());
      }
    });

    app.run(args);
  }
}
