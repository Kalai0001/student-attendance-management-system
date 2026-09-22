package com.college.attendance.repository;

import com.college.attendance.model.Student;
import com.college.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRegisterNumber(String registerNumber);

    Optional<Student> findByUser(User user);

    Optional<Student> findByUser_Username(String username);

    boolean existsByRegisterNumber(String registerNumber);

    @Query("SELECT s FROM Student s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.registerNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:dept IS NULL OR :dept = '' OR s.department = :dept) AND " +
           "(:year IS NULL OR s.year = :year) AND " +
           "(:sec IS NULL OR :sec = '' OR s.section = :sec) " +
           "ORDER BY s.registerNumber ASC")
    List<Student> searchStudents(
            @Param("keyword") String keyword,
            @Param("dept") String dept,
            @Param("year") Integer year,
            @Param("sec") String sec
    );

    @Query("SELECT DISTINCT s.department FROM Student s ORDER BY s.department ASC")
    List<String> findDistinctDepartments();

    @Query("SELECT DISTINCT s.year FROM Student s ORDER BY s.year ASC")
    List<Integer> findDistinctYears();

    @Query("SELECT DISTINCT s.section FROM Student s ORDER BY s.section ASC")
    List<String> findDistinctSections();

    List<Student> findByDepartmentAndYearAndSectionOrderByRegisterNumberAsc(
            String department, Integer year, String section
    );

    List<Student> findTop10ByOrderByIdDesc();
}

