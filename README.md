# 🎓 EduTrack - Student Attendance Management System

A clean, modern, and lightweight **Java Full Stack Web Application** designed for colleges and educational institutions to streamline daily attendance tracking, calculate accurate attendance percentages, flag shortage risks (&lt;75%), and provide dedicated portals for both faculty and students.

---

## 🌟 Key Features

### 👨‍🏫 Admin & Faculty Portal
* **Intuitive Dashboard**: Real-time overview of total enrolled students, total class sessions, today's attendance summary, and immediate shortage alerts.
* **Student Directory & Management**:
  * Register new students with automatic account creation.
  * Search and filter by student name, register number, department, academic year, and section.
  * Edit student information and manage access credentials.
  * Safe deletion with cascading cleanup of linked attendance records.
* **Daily Attendance Marking**:
  * Mark attendance by specific date and class section.
  * Rapid action controls: **"Mark All Present"** and **"Mark All Absent"** buttons.
  * Live status counters.
  * **Strict Duplicate Prevention**: Submitting attendance on an existing date safely updates records without database conflicts or duplicate rows.
* **Attendance History & Logs**:
  * Comprehensive log searchable by date, department, student keyword, and status (Present/Absent).
  * Filtered count summaries (Total records, Present count, Absent count).
* **Class Percentage Report**:
  * Automatically calculates: `Attendance % = (Present Classes / Total Classes) × 100`.
  * Visual progress bars color-coded by performance (&ge;75% Green, &lt;75% Red).
  * Instant defaulter flags for students below the 75% threshold.
  * Printable format support.

### 👨‍🎓 Student Portal
* **Personal Dashboard**:
  * Quick greeting with department, year, and section details.
  * Stat cards for Total Classes Conducted, Classes Attended, Classes Missed, and Overall Attendance Percentage.
  * **Shortage Warning Banner**: Visible alert when attendance falls below the mandatory 75% requirement.
  * Attendance gauge with threshold indicators.
  * Recent class session logs.
* **Profile View**:
  * View complete academic and contact credentials (Register Number, Full Name, Email, Phone, Department, Year, Section, Username).
* **Personal Attendance History**:
  * Full day-by-day attendance history with session details and Present/Absent badges.
  * Printable slip generation.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Backend Framework** | Java 17, Spring Boot 3.2.5 |
| **Data Persistence** | Spring Data JPA, Hibernate ORM |
| **Security & Auth** | Spring Security 6, BCrypt Password Encoder |
| **Database** | MySQL 8.x / 9.x (H2 in-memory profile available) |
| **Template Engine** | Thymeleaf, Thymeleaf Extras Spring Security 6 |
| **Frontend / UI** | HTML5, Vanilla CSS3, JavaScript, Bootstrap 5.3, Bootstrap Icons |
| **Build Tool** | Apache Maven 3.9+ |

---

## 🏗️ Architecture

Layered architecture following best practices:
```
Thymeleaf UI + Bootstrap 5
          ▲
          │ HTTP / Form Submission
          ▼
Spring MVC Controllers (AuthController, AdminController, StudentController)
          ▲
          │
          ▼
Service Layer (StudentService, AttendanceService, CustomUserDetailsService)
          ▲
          │
          ▼
Repository Layer (StudentRepository, AttendanceRepository, UserRepository)
          ▲
          │ JPA / Hibernate
          ▼
MySQL Database (attendance_db)
```

---

## 🚀 Getting Started & Setup Instructions

### 1. Prerequisites
* **Java Development Kit (JDK)**: Version 17 or higher (`java -version`)
* **Apache Maven**: Version 3.8+ (`mvn -v`)
* **MySQL Server**: Version 8.0+ or 9.0+ running on port `3306`

### 2. Database Configuration
By default, the application is configured to connect to MySQL on `localhost:3306`.

Open [`src/main/resources/application.properties`](file:///c:/Users/Admin/SM-System/src/main/resources/application.properties) and verify your MySQL credentials:
```properties
# MySQL DataSource
spring.datasource.url=jdbc:mysql://localhost:3306/attendance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate DDL Auto (creates tables and constraints automatically)
spring.jpa.hibernate.ddl-auto=update
```

> **Note**: If your MySQL `root` password is `root`, no change is needed! If your MySQL user or password is different, simply replace `YOUR_MYSQL_PASSWORD` with your password.

### 3. Build the Application
Run Maven in the project root directory:
```bash
mvn clean package -DskipTests
```
Or run the automated tests:
```bash
mvn clean test
```

### 4. Run the Application
Start the Spring Boot application using:
```bash
mvn spring-boot:run
```

Once started, open your web browser and navigate to:
```
http://localhost:8080
```

---

## 🔑 Administrator Login Credentials

The system initializes the default administrator account on startup:

| Role | Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Faculty / Admin** | `admin` | `admin123` | Full access to manage students, mark daily attendance & view reports |

Student accounts are created when students self-register or when an administrator adds them through the Student Directory. Real attendance records are created only when attendance is marked for a session.

---

## 📋 Database Schema & Relationships

* **`users` Table**:
  * `id` (BIGINT, PK, Auto Increment)
  * `username` (VARCHAR(50), Unique)
  * `password` (VARCHAR(100), BCrypt hash)
  * `role` (VARCHAR(20), `ROLE_ADMIN` or `ROLE_STUDENT`)
  * `enabled` (BOOLEAN)
* **`students` Table**:
  * `id` (BIGINT, PK, Auto Increment)
  * `register_number` (VARCHAR(30), Unique)
  * `name` (VARCHAR(100))
  * `email` (VARCHAR(100))
  * `phone` (VARCHAR(20))
  * `department` (VARCHAR(50))
  * `year` (INT)
  * `section` (VARCHAR(10))
  * `user_id` (BIGINT, FK referencing `users.id`, One-to-One)
* **`attendance` Table**:
  * `id` (BIGINT, PK, Auto Increment)
  * `student_id` (BIGINT, FK referencing `students.id`, Many-to-One)
  * `attendance_date` (DATE)
  * `status` (VARCHAR(15), `PRESENT` or `ABSENT`)
  * **Constraint**: `UNIQUE KEY uk_student_date (student_id, attendance_date)` guarantees no duplicate attendance entries exist for the same student on the same date.

---

## 🧪 Testing & Verification

Automated unit and integration test suite covers:
* Application Context Loading
* Student creation, update, and duplicate register number / username prevention
* Attendance marking and duplicate entry updating (no constraint violation)
* Exact percentage calculation formula: `(Present / Total) * 100`
* Low attendance shortage detection (&lt; 75%)

Run tests anytime with:
```bash
mvn test
```
