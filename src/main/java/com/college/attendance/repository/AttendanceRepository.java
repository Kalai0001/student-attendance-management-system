package com.college.attendance.repository;

import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceStatus;
import com.college.attendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Attendance a WHERE a.student = :student")
    void deleteByStudent(@Param("student") Student student);

    @Modifying
    @Transactional
    @Query("DELETE FROM Attendance a WHERE a.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    Optional<Attendance> findByStudentAndDateAndPeriodAndSubject(Student student, LocalDate date, Integer period, String subject);

    Optional<Attendance> findByStudentIdAndDateAndPeriodAndSubject(Long studentId, LocalDate date, Integer period, String subject);

    Optional<Attendance> findByStudentAndDateAndPeriod(Student student, LocalDate date, Integer period);

    Optional<Attendance> findByStudentAndDate(Student student, LocalDate date);

    Optional<Attendance> findByStudentIdAndDate(Long studentId, LocalDate date);

    boolean existsByStudentAndDate(Student student, LocalDate date);

    List<Attendance> findByDateAndPeriodAndSubject(LocalDate date, Integer period, String subject);

    List<Attendance> findByDate(LocalDate date);

    List<Attendance> findByStudentOrderByDateDescPeriodAsc(Student student);

    List<Attendance> findByStudentIdOrderByDateDesc(Long studentId);

    long countByStudent(Student student);

    long countByStudentAndStatus(Student student, AttendanceStatus status);

    long countByDate(LocalDate date);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    @Query(value = "SELECT COUNT(DISTINCT CONCAT(COALESCE(attendance_date, ''), '-', COALESCE(period, 1), '-', COALESCE(subject, ''))) FROM attendance", nativeQuery = true)
    long countDistinctSessions();

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s WHERE " +
           "(:date IS NULL OR a.date = :date) AND " +
           "(:period IS NULL OR a.period = :period) AND " +
           "(:subject IS NULL OR LOWER(a.subject) LIKE LOWER(CONCAT('%', :subject, '%'))) AND " +
           "(:dept IS NULL OR s.department = :dept) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.registerNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.date DESC, a.period ASC, s.registerNumber ASC")
    List<Attendance> searchAttendanceHistory(
            @Param("date") LocalDate date,
            @Param("period") Integer period,
            @Param("subject") String subject,
            @Param("dept") String dept,
            @Param("status") AttendanceStatus status,
            @Param("keyword") String keyword
    );

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s WHERE a.date = :date AND a.period = :period AND a.subject = :subject AND s.department = :dept AND s.year = :year AND s.section = :sec")
    List<Attendance> findByDateAndClassAndPeriodAndSubject(
            @Param("date") LocalDate date,
            @Param("period") Integer period,
            @Param("subject") String subject,
            @Param("dept") String dept,
            @Param("year") Integer year,
            @Param("sec") String sec
    );
}
