package com.sybyl.trace.web;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class LogDirectoryInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        String path = event.getEnvironment().getProperty("logging.file.name");
        if (path != null) {
            File file = new File(path).getParentFile();
            if (file != null && !file.exists()) {
                boolean created = file.mkdirs();
                if (created) {
                    System.out.println("✅ Created log directory: " + file.getAbsolutePath());
                }
            }
        }
    }
}
