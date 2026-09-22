package com.college.attendance.service;

import com.college.attendance.dto.StudentForm;
import com.college.attendance.dto.StudentRegistrationDto;
import com.college.attendance.model.Role;
import com.college.attendance.model.Student;
import com.college.attendance.model.User;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.StudentRepository;
import com.college.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, 
                          UserRepository userRepository, 
                          AttendanceRepository attendanceRepository,
                          OtpService otpService,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentByRegisterNumber(String regNo) {
        return studentRepository.findByRegisterNumber(regNo);
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentByUsername(String username) {
        return studentRepository.findByUser_Username(username);
    }

    @Transactional(readOnly = true)
    public List<Student> searchStudents(String keyword, String department, Integer year, String section) {
        return studentRepository.searchStudents(keyword, department, year, section);
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByClass(String department, Integer year, String section) {
        return studentRepository.findByDepartmentAndYearAndSectionOrderByRegisterNumberAsc(department, year, section);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctDepartments() {
        return studentRepository.findDistinctDepartments();
    }

    @Transactional(readOnly = true)
    public List<Integer> getDistinctYears() {
        return studentRepository.findDistinctYears();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctSections() {
        return studentRepository.findDistinctSections();
    }

    @Transactional(readOnly = true)
    public long countTotalStudents() {
        return studentRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Student> getRecentStudents() {
        return studentRepository.findTop10ByOrderByIdDesc();
    }

    public Student createStudent(StudentForm form) {
        if (studentRepository.existsByRegisterNumber(form.getRegisterNumber())) {
            throw new IllegalArgumentException("A student with Register Number '" + form.getRegisterNumber() + "' already exists.");
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username '" + form.getUsername() + "' is already taken.");
        }

        String rawPassword = (form.getPassword() != null && !form.getPassword().trim().isEmpty())
                ? form.getPassword()
                : "student123";

        User user = new User(form.getUsername(), passwordEncoder.encode(rawPassword), Role.ROLE_STUDENT);
        userRepository.save(user);

        Student student = new Student(
                form.getRegisterNumber().toUpperCase().trim(),
                form.getName().trim(),
                form.getEmail().trim(),
                form.getPhone() != null ? form.getPhone().trim() : null,
                form.getDepartment(),
                form.getYear(),
                form.getSection(),
                user
        );

        return studentRepository.save(student);
    }

    public Student updateStudent(Long id, StudentForm form) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));

        // Check if register number changed and already taken by someone else
        if (!existingStudent.getRegisterNumber().equalsIgnoreCase(form.getRegisterNumber().trim())) {
            if (studentRepository.existsByRegisterNumber(form.getRegisterNumber().trim())) {
                throw new IllegalArgumentException("Register Number '" + form.getRegisterNumber() + "' is already in use.");
            }
            existingStudent.setRegisterNumber(form.getRegisterNumber().toUpperCase().trim());
        }

        existingStudent.setName(form.getName().trim());
        if (!existingStudent.getEmail().equalsIgnoreCase(form.getEmail().trim())) {
            existingStudent.setEmail(form.getEmail().trim());
            existingStudent.setEmailVerified(false);
        }
        existingStudent.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        existingStudent.setDepartment(form.getDepartment());
        existingStudent.setYear(form.getYear());
        existingStudent.setSection(form.getSection());

        // Update User credentials
        User user = existingStudent.getUser();
        if (user != null) {
            if (!user.getUsername().equalsIgnoreCase(form.getUsername().trim())) {
                if (userRepository.existsByUsername(form.getUsername().trim())) {
                    throw new IllegalArgumentException("Username '" + form.getUsername() + "' is already taken.");
                }
                user.setUsername(form.getUsername().trim());
            }
            if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(form.getPassword().trim()));
            }
            userRepository.save(user);
        }

        return studentRepository.save(existingStudent);
    }

    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));

        User user = student.getUser();
        String username = (user != null) ? user.getUsername() : null;

        // 1. Delete all attendance records associated with this student
        student.getAttendances().clear();
        attendanceRepository.deleteByStudent(student);
        attendanceRepository.flush();

        // 2. Permanently delete student record
        studentRepository.delete(student);
        studentRepository.flush();

        // 3. Permanently delete associated user account
        if (user != null) {
            userRepository.delete(user);
            userRepository.flush();
        }

        // 4. Clear any pending or cached OTP for this student
        if (username != null) {
            otpService.clearOtp(username);
        }
    }

    public void markEmailAsVerified(String username) {
        Student student = getStudentByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student not found for username: " + username));
        student.setEmailVerified(true);
        studentRepository.save(student);
    }

    public Student registerStudent(StudentRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        String trimmedRegNo = dto.getRegisterNumber().trim();
        if (studentRepository.existsByRegisterNumber(trimmedRegNo) || 
            studentRepository.existsByRegisterNumber(trimmedRegNo.toUpperCase())) {
            throw new IllegalArgumentException("A student with ID '" + trimmedRegNo + "' already exists.");
        }
        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername().trim() + "' is already taken.");
        }

        // New student account created as PENDING (enabled = false)
        User user = new User(dto.getUsername().trim(), passwordEncoder.encode(dto.getPassword().trim()), Role.ROLE_STUDENT);
        user.setEnabled(false); // Requires Admin approval
        userRepository.save(user);

        Student student = new Student(
                dto.getRegisterNumber().toUpperCase().trim(),
                dto.getName().trim(),
                dto.getEmail().trim(),
                dto.getPhone() != null ? dto.getPhone().trim() : null,
                dto.getDepartment(),
                dto.getYear() != null ? dto.getYear() : 1,
                "", // No section as per user request
                user
        );

        return studentRepository.save(student);
    }

    public void approveStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        if (student.getUser() != null) {
            student.getUser().setEnabled(true);
            userRepository.save(student.getUser());
        }
    }

    public void rejectStudent(Long studentId) {
        deleteStudent(studentId);
    }

    public StudentForm toForm(Student student) {
        StudentForm form = new StudentForm();
        form.setId(student.getId());
        form.setRegisterNumber(student.getRegisterNumber());
        form.setName(student.getName());
        form.setEmail(student.getEmail());
        form.setPhone(student.getPhone());
        form.setDepartment(student.getDepartment());
        form.setYear(student.getYear());
        form.setSection(student.getSection());
        if (student.getUser() != null) {
            form.setUsername(student.getUser().getUsername());
        }
        return form;
    }
}
