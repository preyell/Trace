# TRACE - Enterprise Order & Expense Management System

Trace is a secure, Spring Boot-based backend application designed to track sales orders, associated financial commitments, and resulting margins. It features built-in audit trails, passwordless authentication.

* **Secure Passwordless Authentication:** Email-based OTP login with deep-linking support for direct order access.
* **Order & Margin Management:** End-to-end tracking of sales orders, margin reports, and additional expenses.
* **Consumption Tracking:** Granular tracking mechanism restricted to D&I (Delivery & Installation) expenses with precise "Consumed On" dates.
* **Comprehensive Audit Trails:** System-wide auditing with mandatory comment capture for approval/rejection workflows.
* **Automated AWS S3 Backups:** Nightly, encrypted PostgreSQL database dumps uploaded directly to Amazon S3.
* **Dynamic Master Data:** Role-based access control where disabled users, customers, and verticals are gracefully hidden from active workflows.


## Technology Stack

* **Runtime:** Java 21 (Amazon Corretto )
* **Framework:** Spring Boot 
* **Database:** PostgreSQL 17 (Hosted on Amazon RDS)
* **Cloud Infrastructure:** AWS EC2 (Windows Server)
* **Service Management:** WinSW (Windows Service Wrapper)

## 🏗️ Server Environment & Prerequisites

To deploy or migrate this application, the Windows Server must have the following installed and configured:

1.  **Java 21 JRE/JDK:** Must be installed and added to the system `PATH`.
2.  **PostgreSQL 17 Command Line Tools:** The `pg_dump.exe` utility is strictly required for the automated backup service to function.

### Directory Structure
The application expects the following directory structure on the host `C:` drive:


C:\
├── TraceApp\                                # Application Root
│   ├── trace.jar                            # Compiled Spring Boot Application
│   ├── application-prod.properties          # External Configuration File
│   ├── TraceService.exe                     # WinSW Executable
│   └── TraceService.xml                     # WinSW Configuration
├── trace_docs\                              # Uploaded Documents
│   ├── margin-reports\
│   ├── additional-expenses\
│   └── invoices\
├── trace_log\                               # Application Logs
└── trace_backups\                           # Temporary directory for DB dumps prior to S3 upload

### Configuration
The application uses Spring Boot's override hierarchy. All sensitive credentials and environment-specific variables are managed via the external application-prod.properties file located at C:\TraceApp\application-prod.properties.

Critical configurations to maintain:

spring.datasource.*: Amazon RDS connection string and credentials.

spring.mail.*: SMTP credentials (Requires an App Password if using Google/Gmail).

app.public-base-url: The Static Elastic IP or Domain Name used for email deep-links.

### Deployment & Operations
The Trace application runs as a background Windows Service to ensure it survives user logouts and server reboots.

### Starting and Stopping the Application
You can manage the application using the standard Windows Services interface (services.msc) by looking for Trace Enterprise Application, or via an Administrator Command Prompt:

Start: net start TraceApp

Stop: net stop TraceApp

Restart: net stop TraceApp && net start TraceApp

### Updating the Application (Deploying a new JAR)
If a new version of trace.jar is provided, follow these steps to update the server:

Open an Administrator Command Prompt.

Stop the service: net stop TraceApp

Replace the old trace.jar in C:\TraceApp\ with the new version.

Start the service: net start TraceApp

### 🔍 Troubleshooting & Logs
If the application fails to start or an error occurs in production, do not check the Windows Event Viewer. The application maintains its own detailed logs.

Log Location: C:\trace_log\trace-app.log

Log Rotation: Logs will automatically roll over and archive to prevent filling the server's hard drive.
