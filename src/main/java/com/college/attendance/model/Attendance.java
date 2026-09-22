package com.college.attendance.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_date_period_subject", columnNames = {"student_id", "attendance_date", "period", "subject"})
}, indexes = {
    @Index(name = "idx_attendance_date", columnList = "attendance_date"),
    @Index(name = "idx_attendance_student", columnList = "student_id"),
    @Index(name = "idx_attendance_session", columnList = "attendance_date, period, subject")
})
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    @Column(name = "period", nullable = false)
    private Integer period = 1;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject = "General";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    public Attendance() {
    }

    public Attendance(Student student, LocalDate date, Integer period, String subject, AttendanceStatus status) {
        this.student = student;
        this.date = date;
        this.period = period != null ? period : 1;
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
        this.status = status != null ? status : AttendanceStatus.ABSENT;
    }

    public Attendance(Student student, LocalDate date, AttendanceStatus status) {
        this(student, date, 1, "General", status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPeriod() {
        return period != null ? period : 1;
    }

    public void setPeriod(Integer period) {
        this.period = period != null ? period : 1;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
