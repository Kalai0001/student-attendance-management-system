package com.college.attendance;

import com.college.attendance.dto.StudentForm;
import com.college.attendance.model.Student;
import com.college.attendance.service.StudentService;
import com.college.attendance.dto.StudentRegistrationDto;
import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceStatus;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.StudentRepository;
import com.college.attendance.repository.UserRepository;
import com.college.attendance.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceService attendanceService;

    @Test
    void testCreateStudentSuccess() {
        StudentForm form = new StudentForm();
        form.setRegisterNumber("REG2024TEST01");
        form.setName("Test Student");
        form.setEmail("test01@college.edu");
        form.setPhone("9876500001");
        form.setDepartment("Information Technology");
        form.setYear(1);
        form.setSection("A");
        form.setUsername("unique_user_01");
        form.setPassword("password123");

        Student created = studentService.createStudent(form);
        assertNotNull(created.getId());
        assertEquals("REG2024TEST01", created.getRegisterNumber());
        assertEquals("unique_user_01", created.getUser().getUsername());
    }

    @Test
    void testDuplicateRegisterNumberThrowsException() {
        StudentForm form1 = new StudentForm();
        form1.setRegisterNumber("REG_DUP_01");
        form1.setName("Student One");
        form1.setEmail("one@college.edu");
        form1.setDepartment("Computer Science");
        form1.setYear(2);
        form1.setSection("A");
        form1.setUsername("user_one");
        form1.setPassword("pwd");
        studentService.createStudent(form1);

        StudentForm form2 = new StudentForm();
        form2.setRegisterNumber("REG_DUP_01"); // Duplicate!
        form2.setName("Student Two");
        form2.setEmail("two@college.edu");
        form2.setDepartment("Computer Science");
        form2.setYear(2);
        form2.setSection("B");
        form2.setUsername("user_two");
        form2.setPassword("pwd");

        assertThrows(IllegalArgumentException.class, () -> studentService.createStudent(form2));
    }

    @Test
    void testDuplicateUsernameThrowsException() {
        StudentForm form1 = new StudentForm();
        form1.setRegisterNumber("REG_USER_01");
        form1.setName("Student One");
        form1.setEmail("user1@college.edu");
        form1.setDepartment("Computer Science");
        form1.setYear(2);
        form1.setSection("A");
        form1.setUsername("duplicate_username");
        form1.setPassword("pwd");
        studentService.createStudent(form1);

        StudentForm form2 = new StudentForm();
        form2.setRegisterNumber("REG_USER_02");
        form2.setName("Student Two");
        form2.setEmail("user2@college.edu");
        form2.setDepartment("Computer Science");
        form2.setYear(2);
        form2.setSection("B");
        form2.setUsername("duplicate_username"); // Duplicate username!
        form2.setPassword("pwd");

        assertThrows(IllegalArgumentException.class, () -> studentService.createStudent(form2));
    }

    @Test
    void testDeleteStudentPermanentlyRemovesStudentUserAndAttendance() {
        // 1. Create a student
        StudentForm form = new StudentForm();
        form.setRegisterNumber("REG_DEL_01");
        form.setName("Student To Delete");
        form.setEmail("delete_me@college.edu");
        form.setDepartment("Mechanical");
        form.setYear(3);
        form.setSection("A");
        form.setUsername("user_to_delete");
        form.setPassword("secret123");
        Student student = studentService.createStudent(form);
        Long studentId = student.getId();
        String username = student.getUser().getUsername();

        // 2. Add attendance records for this student
        attendanceService.markOrUpdateAttendance(studentId, LocalDate.of(2026, 9, 1), AttendanceStatus.PRESENT);
        attendanceService.markOrUpdateAttendance(studentId, LocalDate.of(2026, 9, 2), AttendanceStatus.ABSENT);

        assertEquals(2, attendanceRepository.findByStudentIdOrderByDateDesc(studentId).size());
        assertTrue(studentRepository.existsById(studentId));
        assertTrue(userRepository.existsByUsername(username));

        // 3. Delete student as admin
        studentService.deleteStudent(studentId);

        // 4. Verify all traces are completely removed
        assertFalse(studentRepository.existsById(studentId), "Student record should be permanently deleted from database");
        assertFalse(userRepository.existsByUsername(username), "User/login record should be completely removed");
        assertEquals(0, attendanceRepository.findByStudentIdOrderByDateDesc(studentId).size(), "Attendance records should be deleted");
    }

    @Test
    void testReRegistrationAfterStudentDeletionSucceedsAsNewPendingAccount() {
        String regNo = "REG_RE_REGISTER_01";
        String username = "rereg_user_01";
        String email = "rereg@college.edu";

        // 1. Initially register a student through student sign up
        StudentRegistrationDto dto = new StudentRegistrationDto();
        dto.setRegisterNumber(regNo);
        dto.setName("Original Student");
        dto.setEmail(email);
        dto.setPhone("9988776655");
        dto.setUsername(username);
        dto.setPassword("initial_pass_123");
        dto.setConfirmPassword("initial_pass_123");
        dto.setDepartment("Civil");
        dto.setYear(1);

        Student registered = studentService.registerStudent(dto);
        Long initialStudentId = registered.getId();
        assertNotNull(initialStudentId);
        assertFalse(registered.getUser().isEnabled(), "Initial account is PENDING");
        assertFalse(registered.isEmailVerified(), "Initial emailVerified is false");

        // Admin approves student
        studentService.approveStudent(initialStudentId);
        assertTrue(userRepository.findByUsername(username).get().isEnabled());

        // Student verifies email
        studentService.markEmailAsVerified(username);
        assertTrue(studentRepository.findById(initialStudentId).get().isEmailVerified());

        // 2. Admin deletes student
        studentService.deleteStudent(initialStudentId);

        assertFalse(studentRepository.existsById(initialStudentId));
        assertFalse(userRepository.existsByUsername(username));

        // 3. Student registers AGAIN with the EXACT same student ID, username, and email
        StudentRegistrationDto newDto = new StudentRegistrationDto();
        newDto.setRegisterNumber(regNo);
        newDto.setName("New Student Same ID");
        newDto.setEmail(email);
        newDto.setPhone("9988776655");
        newDto.setUsername(username);
        newDto.setPassword("new_pass_456");
        newDto.setConfirmPassword("new_pass_456");
        newDto.setDepartment("Civil");
        newDto.setYear(1);

        Student reRegistered = studentService.registerStudent(newDto);
        assertNotNull(reRegistered.getId());
        assertNotEquals(initialStudentId, reRegistered.getId(), "Should be a new student record with new ID");
        assertEquals(regNo.toUpperCase(), reRegistered.getRegisterNumber());
        assertEquals(username, reRegistered.getUser().getUsername());
        assertEquals(email, reRegistered.getEmail());

        // 4. Verify new account properties:
        assertFalse(reRegistered.getUser().isEnabled(), "Re-registered account must start as PENDING admin approval");
        assertFalse(reRegistered.isEmailVerified(), "Re-registered account must start with emailVerified = false");
    }

    @Autowired
    private com.college.attendance.service.CustomUserDetailsService customUserDetailsService;

    @Test
    void testDeletedStudentOldAccountCannotAuthenticate() {
        StudentForm form = new StudentForm();
        form.setRegisterNumber("REG_AUTH_01");
        form.setName("Auth Student");
        form.setEmail("auth@college.edu");
        form.setDepartment("Computer Science");
        form.setYear(1);
        form.setSection("A");
        form.setUsername("auth_user_del");
        form.setPassword("somePassword");
        Student student = studentService.createStudent(form);
        String username = student.getUser().getUsername();

        // User can be loaded before deletion
        assertNotNull(customUserDetailsService.loadUserByUsername(username));

        // Admin deletes student
        studentService.deleteStudent(student.getId());

        // Attempting to load user after deletion throws UsernameNotFoundException
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(username);
        });
    }

    @Test
    void testRejectPendingStudentRemovesAccountAndAllowsReRegistration() {
        String regNo = "REG_REJECT_01";
        String username = "reject_user_01";
        String email = "reject@college.edu";

        StudentRegistrationDto dto = new StudentRegistrationDto();
        dto.setRegisterNumber(regNo);
        dto.setName("Pending Reject Student");
        dto.setEmail(email);
        dto.setPhone("9988771122");
        dto.setUsername(username);
        dto.setPassword("pwd12345");
        dto.setConfirmPassword("pwd12345");
        dto.setDepartment("ECE");
        dto.setYear(2);

        Student registered = studentService.registerStudent(dto);
        Long studentId = registered.getId();
        assertTrue(studentRepository.existsById(studentId));
        assertTrue(userRepository.existsByUsername(username));

        // Admin rejects student
        studentService.rejectStudent(studentId);

        assertFalse(studentRepository.existsById(studentId));
        assertFalse(userRepository.existsByUsername(username));

        // Can register again cleanly
        Student reRegistered = studentService.registerStudent(dto);
        assertNotNull(reRegistered.getId());
        assertNotEquals(studentId, reRegistered.getId());
    }
}
