package com.sybyl.trace.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseBackupService {

    private final JavaMailSender mailSender;
    private final Environment env;

    @Value("${app.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${app.backup.cron:0 30 2 * * *}")
    private String cron;

    @Value("${app.backup.recipient}")
    private String recipient;

    @Value("${app.backup.from:no-reply@localhost}")
    private String from;

    @Value("${app.backup.temp-dir:}")
    private String tempDir;

    @Value("${app.backup.pg-dump-path:pg_dump}")
    private String pgDumpPath;

    private Path backupRootDir;

    @PostConstruct
    public void init() {
        if (!backupEnabled) {
            log.info("Database backup scheduler is DISABLED (app.backup.enabled=false)");
            return;
        }

        if (!StringUtils.hasText(recipient)) {
            log.warn("Database backup: app.backup.recipient is not configured. Backups will NOT be emailed.");
        }

        if (!StringUtils.hasText(tempDir)) {
            // default: system temp directory
            backupRootDir = Path.of(System.getProperty("java.io.tmpdir"), "trace_db_backups");
        } else {
            backupRootDir = Path.of(tempDir);
        }

        try {
            Files.createDirectories(backupRootDir);
            log.info("Database backup directory initialised at {}", backupRootDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create backup directory at {}. Backups may fail.", backupRootDir, e);
        }

        log.info("Database backup scheduler initialised. Cron={}, pgDumpPath={}, enabled={}",
                cron, pgDumpPath, backupEnabled);
    }

    /**
     * Scheduled job – time controlled by trace.backup.cron.
     */
    @Scheduled(cron = "${app.backup.cron:0 30 2 * * *}")
    public void scheduledBackup() {
        if (!backupEnabled) {
            return;
        }

        log.info("Starting scheduled database backup job...");

        Path backupFile = null;
        try {
            backupFile = createBackupFilePath();
            runPgDump(backupFile);
            log.info("Database backup created at {}", backupFile.toAbsolutePath());

            if (StringUtils.hasText(recipient)) {
                sendBackupEmail(backupFile);
                log.info("Database backup emailed to {}", recipient);
            } else {
                log.warn("Database backup recipient not configured, skipping email send.");
            }

        } catch (Exception ex) {
            log.error("Database backup job FAILED", ex);
        } finally {
            // Optional: clean up file after email is sent
            if (backupFile != null && Files.exists(backupFile)) {
                try {
                    Files.delete(backupFile);
                    log.debug("Deleted temporary backup file {}", backupFile.toAbsolutePath());
                } catch (IOException e) {
                    log.warn("Failed to delete temporary backup file {}", backupFile.toAbsolutePath(), e);
                }
            }
        }
    }

    private Path createBackupFilePath() throws IOException {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dbName = env.getProperty("spring.datasource.name",
                env.getProperty("spring.datasource.hikari.pool-name", "trace_db"));

        String fileName = String.format("trace_backup_%s_%s.dump", dbName, timestamp);
        return backupRootDir.resolve(fileName);
    }

    /**
     * Executes pg_dump to create a backup file.
     */
    private void runPgDump(Path outputFile) throws IOException, InterruptedException {
        String host = env.getProperty("spring.datasource.host",
                env.getProperty("spring.datasource.url"));
        String port = env.getProperty("spring.datasource.port", "5432");
        String dbName = env.getProperty("spring.datasource.dbname",
                env.getProperty("spring.datasource.name", "trace"));
        String user = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        if (!StringUtils.hasText(user) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("DB username/password not configured (spring.datasource.username / password).");
        }

        // If spring.datasource.url is in jdbc:postgresql://host:port/db format, try to parse host & db
        if (host != null && host.startsWith("jdbc:postgresql://")) {
            try {
                // jdbc:postgresql://host:port/dbName
                String url = host.substring("jdbc:postgresql://".length());
                String[] hostAndDb = url.split("/", 2);
                String hostPort = hostAndDb[0];
                if (hostAndDb.length > 1) {
                    dbName = hostAndDb[1];
                }
                String[] hp = hostPort.split(":", 2);
                host = hp[0];
                if (hp.length > 1) {
                    port = hp[1];
                }
            } catch (Exception e) {
                log.warn("Could not parse spring.datasource.url for host/port/db, using defaults. url={}", host, e);
            }
        }

        log.info("Running pg_dump for db={} on {}:{} -> {}", dbName, host, port, outputFile);

        ProcessBuilder pb = new ProcessBuilder(
                pgDumpPath,
                "-h", host,
                "-p", port,
                "-U", user,
                "-F", "c",                // custom format
                "-b",                    // include blobs
                "-f", outputFile.toString(),
                dbName
        );

        // Set PGPASSWORD so pg_dump can authenticate without prompting
        Map<String, String> envVars = pb.environment();
        envVars.put("PGPASSWORD", password);

        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IllegalStateException("pg_dump exited with code " + exitCode);
        }
    }

    private void sendBackupEmail(Path backupFile) throws MessagingException {
        File file = backupFile.toFile();
        if (!file.exists()) {
            throw new IllegalStateException("Backup file does not exist: " + file.getAbsolutePath());
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(from);
        helper.setTo(recipient);

        String subject = "TRACE DB Backup - " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        helper.setSubject(subject);

        helper.setText("""
                This is an automated email from TRACE.

                Attached is the latest PostgreSQL database backup.

                Please store this securely.

                """, false);

        helper.addAttachment(file.getName(), new FileSystemResource(file));

        mailSender.send(message);
    }
}
