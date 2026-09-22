package com.college.attendance.config;

import com.college.attendance.model.Role;
import com.college.attendance.model.Student;
import com.college.attendance.model.User;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.StudentRepository;
import com.college.attendance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository,
                           StudentRepository studentRepository,
                           AttendanceRepository attendanceRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Drop old single-date constraint from MySQL if it exists to allow 8 periods & multiple subjects
        try {
            jdbcTemplate.execute("ALTER TABLE attendance DROP INDEX uk_student_date");
            log.info("Dropped legacy index uk_student_date");
        } catch (Exception ignored) {
            // Index might not exist or already dropped
        }

        try {
            jdbcTemplate.execute("ALTER TABLE attendance DROP INDEX idx_attendance_student_date");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE attendance MODIFY COLUMN status VARCHAR(50) NOT NULL");
        } catch (Exception ignored) {}

        try {
            // Backfill any null period or subject in existing DB records
            jdbcTemplate.execute("UPDATE attendance SET period = 1 WHERE period IS NULL");
            jdbcTemplate.execute("UPDATE attendance SET subject = 'General' WHERE subject IS NULL OR subject = ''");
        } catch (Exception e) {
            log.warn("Database cleanup note: {}", e.getMessage());
        }

        // 2. Ensure admin account exists with admin123
        userRepository.findByUsername("admin").ifPresentOrElse(admin -> {
            if (!passwordEncoder.matches("admin123", admin.getPassword()) &&
                !passwordEncoder.matches("admin098", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("admin123"));
                userRepository.save(admin);
            }
        }, () -> {
            User admin = new User("admin", passwordEncoder.encode("admin123"), Role.ROLE_ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Created default admin user (admin / admin123)");
        });

        // 3. Clean up legacy demo students and their dummy attendance data if present
        List<String> dummyRegNos = List.of("REG2026001", "REG2026002", "REG2026003", "REG2026004", "REG2026005");
        for (String regNo : dummyRegNos) {
            studentRepository.findByRegisterNumber(regNo).ifPresent(dummyStudent -> {
                User dummyUser = dummyStudent.getUser();
                attendanceRepository.deleteByStudent(dummyStudent);
                studentRepository.delete(dummyStudent);
                if (dummyUser != null) {
                    userRepository.delete(dummyUser);
                }
                log.info("Cleaned up legacy dummy student record: {}", regNo);
            });
        }

        List<String> dummyUsernames = List.of("student1", "student2", "student3", "student4", "student5");
        for (String username : dummyUsernames) {
            userRepository.findByUsername(username).ifPresent(dummyUser -> {
                studentRepository.findByUser_Username(username).ifPresent(dummyStudent -> {
                    attendanceRepository.deleteByStudent(dummyStudent);
                    studentRepository.delete(dummyStudent);
                });
                userRepository.delete(dummyUser);
                log.info("Cleaned up legacy dummy user: {}", username);
            });
        }

        try {
            jdbcTemplate.execute("DELETE FROM attendance WHERE student_id NOT IN (SELECT id FROM students)");
        } catch (Exception ignored) {}
    }
}
