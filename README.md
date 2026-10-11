# LADO Manufacturing Management System

A web-based manufacturing management application developed as a college project to manage business operations, inventory, transactions, and reports.

## Project Overview

The LADO Manufacturing Management System is designed to help a manufacturing business organize its day-to-day activities through a centralized web application. It provides a structured way to manage products, customers, suppliers, purchases, sales, payments, accounts, and inventory.

The application uses Java and Spring Boot for the backend, Oracle Database for data storage, and HTML, CSS, and JavaScript for the frontend.

## Objectives

- Centralize manufacturing business records in a database.
- Manage product and inventory information.
- Maintain customer and supplier records.
- Record purchase and sales transactions.
- Track payments and business accounts.
- Generate reports for business operations.
- Provide login and role-based access functionality.
- Support company-wise data management.

## Technology Stack

| Component | Technology |
|---|---|
| Backend | Java 21 |
| Backend framework | Spring Boot |
| Build tool | Maven |
| Database | Oracle Database Free |
| Database access | Spring Data JPA / Hibernate |
| Frontend | HTML, CSS, JavaScript |
| Security | Spring Security |
| Database connectivity | Oracle JDBC Driver |
| Development environment | Visual Studio Code |
| Version control | Git |
| Source code hosting | GitHub |

## Main Modules

### 1. Dashboard
Provides an interface for accessing the application's main business functions.

### 2. Product Management
Manages product information, categories, units, and related records.

### 3. Inventory Management
Maintains inventory records and supports stock-related operations.

### 4. Customer Management
Stores and manages customer information.

### 5. Supplier Management
Maintains supplier information and related business records.

### 6. Purchase Management
Records purchase transactions and their associated items.

### 7. Sales Management
Records sales transactions and associated sale items.

### 8. Payment Management
Manages payment records associated with business transactions.

### 9. Accounts Management
Supports the management of business account information.

### 10. Reports
Provides reporting functionality for business records and transactions.

### 11. User and Company Management
Includes user authentication and company-related functionality. Company-wise access should be verified across all modules before production use.

## Project Structure

```text
lado-manufacturing/
├── .mvn/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── lado/com/lado_manufacturing/
│   │   │       ├── Controllers
│   │   │       ├── Entities
│   │   │       ├── Repositories
│   │   │       ├── SecurityConfig.java
│   │   │       └── LadoManufacturingApplication.java
│   │   └── resources/
│   │       ├── application.properties.example
│   │       └── static/
│   │           ├── HTML pages
│   │           ├── JavaScript files
│   │           └── CSS stylesheets
│   └── test/
└── README.md
```

*The folder names above describe the main code groupings; see the actual source tree for individual files.*

## Prerequisites

Install or prepare the following before running the application:

- Java Development Kit (JDK) 21
- Docker Desktop
- Oracle Database Free running in Docker, or a compatible Oracle database
- A terminal and a code editor such as Visual Studio Code

## Database Configuration

The application is configured to connect to Oracle Database using environment variables.

The example configuration file is:

`src/main/resources/application.properties.example`

The default connection URL in the example is:

```properties
jdbc:oracle:thin:@localhost:1521/FREEPDB1
```

Configure these environment variables in your local terminal:

```bash
export DB_URL='jdbc:oracle:thin:@localhost:1521/FREEPDB1'
export DB_USERNAME='LADO_MANUFACTURING'
export DB_PASSWORD='YOUR_DATABASE_PASSWORD'
```

Replace `YOUR_DATABASE_PASSWORD` with your own local Oracle schema password. Never commit real database passwords or other secrets to GitHub.

The database container, pluggable database, schema, tables, and required reference data must be configured before the application can run successfully.

## Running the Application

### 1. Open the project directory

```bash
cd ~/Desktop/lado-manufacturing
```

### 2. Start Oracle Database

Start your configured Oracle Database Free container using Docker Desktop if it is not already running.

### 3. Set database environment variables

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the terminal where you will launch the application.

### 4. Start the Spring Boot application

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

On Windows:

```bat
mvnw.cmd spring-boot:run
```

### 5. Open the application

After Spring Boot starts successfully, open:

http://localhost:8080

The available pages and authentication requirements depend on the application's current configuration.

## Security Notes

- Keep `application.properties` and other files containing real credentials out of version control.
- Use environment variables for sensitive configuration.
- Do not publish database passwords, access tokens, private keys, or real customer and company data.
- Configure strong administrator passwords.
- Review authorization rules and company-wise data isolation before deploying the application.
- Do not expose the database port or application to untrusted networks without appropriate security controls.

## Current Project Status

The initial source-code version is committed to Git and hosted on GitHub. The application includes the main backend classes and frontend pages for the modules described above.

Database setup, integration testing, security validation, and deployment readiness should be verified in the target environment.

## Future Enhancements

- Add advanced inventory and stock movement tracking.
- Improve financial and transaction reporting.
- Add data export and printable reports.
- Strengthen company-level authorization and audit logging.
- Add automated testing for business rules and access controls.
- Improve deployment documentation and backup procedures.
- Add dashboards and analytics based on verified business data.

## Academic Project

**Project:** LADO Manufacturing Management System  
**Type:** College project  
**Backend:** Java and Spring Boot  
**Database:** Oracle Database  
**Frontend:** HTML, CSS, and JavaScript  
**Version Control:** Git and GitHub
