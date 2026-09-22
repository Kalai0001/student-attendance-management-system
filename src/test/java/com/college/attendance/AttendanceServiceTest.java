package com.college.attendance;

import com.college.attendance.dto.AttendanceSheetForm;
import com.college.attendance.dto.AttendanceStatsDto;
import com.college.attendance.dto.StudentForm;
import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceStatus;
import com.college.attendance.model.Student;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.service.AttendanceService;
import com.college.attendance.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AttendanceServiceTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        StudentForm form = new StudentForm();
        form.setRegisterNumber("TESTREG101");
        form.setName("Unit Test Student");
        form.setEmail("unittest@college.edu");
        form.setPhone("9998887770");
        form.setDepartment("Computer Science");
        form.setYear(2);
        form.setSection("A");
        form.setUsername("unittest_user");
        form.setPassword("password123");

        testStudent = studentService.createStudent(form);
    }

    @Test
    void testMarkAttendanceAndPreventDuplicateOnSameDatePeriodAndSubject() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        int period = 1;
        String subject = "Data Structures";

        // First marking: PRESENT
        Attendance att1 = attendanceService.markOrUpdateAttendance(testStudent.getId(), date, period, subject, AttendanceStatus.PRESENT);
        assertNotNull(att1.getId());
        assertEquals(AttendanceStatus.PRESENT, att1.getStatus());
        assertEquals(period, att1.getPeriod());
        assertEquals(subject, att1.getSubject());

        // Count for this student should be 1
        long countBefore = attendanceRepository.countByStudent(testStudent);
        assertEquals(1, countBefore);

        // Second marking on same date + period + subject: update to ABSENT
        Attendance att2 = attendanceService.markOrUpdateAttendance(testStudent.getId(), date, period, subject, AttendanceStatus.ABSENT);
        assertEquals(att1.getId(), att2.getId(), "Should update existing record ID rather than creating a duplicate");
        assertEquals(AttendanceStatus.ABSENT, att2.getStatus());

        // Total count must still be 1 (no duplicate!)
        long countAfter = attendanceRepository.countByStudent(testStudent);
        assertEquals(1, countAfter);

        // Marking a different period (Period 2) on the same date creates a new record
        Attendance attPeriod2 = attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 2, "Operating Systems", AttendanceStatus.PRESENT);
        assertNotNull(attPeriod2.getId());
        assertNotEquals(att1.getId(), attPeriod2.getId());
        assertEquals(2, attendanceRepository.countByStudent(testStudent));
    }

    @Test
    void testDefaultStatusIsAbsentWhenCreatingSession() {
        LocalDate date = LocalDate.of(2026, 9, 5);
        AttendanceSheetForm sheet = attendanceService.prepareSheetForClass(date, 3, "Algorithms", "Computer Science", 2, "A");
        assertNotNull(sheet);
        assertFalse(sheet.getRecords().isEmpty());
        // All students in new session must default to ABSENT
        sheet.getRecords().forEach(rec -> assertEquals(AttendanceStatus.ABSENT, rec.getStatus()));
    }

    @Test
    void testAttendancePercentageFormulaWithOdAndLeave() {
        // Formula: Attendance % = (Present + OD) / (Total Classes - Granted Leave) * 100
        LocalDate date = LocalDate.of(2026, 9, 10);

        // Period 1: PRESENT
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 1, "Sub 1", AttendanceStatus.PRESENT);
        // Period 2: OD (Attended)
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 2, "Sub 2", AttendanceStatus.OD);
        // Period 3: ABSENT (Not Attended)
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 3, "Sub 3", AttendanceStatus.ABSENT);
        // Period 4: LEAVE_DENIED (Treated as Absent -> In denominator, not in numerator)
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 4, "Sub 4", AttendanceStatus.LEAVE_DENIED);
        // Period 5: LEAVE_GRANTED (Excluded from calculation -> Subtracted from denominator)
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 5, "Sub 5", AttendanceStatus.LEAVE_GRANTED);

        AttendanceStatsDto stats = attendanceService.getStudentStats(testStudent);
        assertEquals(5, stats.getTotalClasses());
        assertEquals(1, stats.getPresentClasses());
        assertEquals(1, stats.getOdClasses());
        assertEquals(1, stats.getAbsentClasses());
        assertEquals(1, stats.getLeaveDeniedClasses());
        assertEquals(1, stats.getLeaveGrantedClasses());

        // Effective denominator = 5 - 1 = 4
        // Attended numerator = 1 (Present) + 1 (OD) = 2
        // Percentage = 2 / 4 * 100 = 50.0%
        assertEquals(50.0, stats.getPercentage(), 0.01);
        assertTrue(stats.isLowAttendance(), "50% is below 75%, should be flagged as low attendance");

        // Add 2 more PRESENT classes (Periods 6 & 7):
        // Total = 7, Leave Granted = 1 -> Effective Denominator = 6
        // Attended = 2 (old) + 2 (new) = 4
        // Percentage = 4 / 6 * 100 = 66.7%
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 6, "Sub 6", AttendanceStatus.PRESENT);
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 7, "Sub 7", AttendanceStatus.PRESENT);

        AttendanceStatsDto updatedStats = attendanceService.getStudentStats(testStudent);
        assertEquals(7, updatedStats.getTotalClasses());
        assertEquals(3, updatedStats.getPresentClasses());
        assertEquals(1, updatedStats.getOdClasses());
        assertEquals(66.7, updatedStats.getPercentage(), 0.01);
        assertTrue(updatedStats.isLowAttendance());

        // Add 2 more PRESENT classes (Period 8 and next day Period 1):
        // Total = 9, Leave Granted = 1 -> Effective Denominator = 8
        // Attended = 4 + 2 = 6
        // Percentage = 6 / 8 * 100 = 75.0%
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date, 8, "Sub 8", AttendanceStatus.PRESENT);
        attendanceService.markOrUpdateAttendance(testStudent.getId(), date.plusDays(1), 1, "Sub 1", AttendanceStatus.PRESENT);

        AttendanceStatsDto finalStats = attendanceService.getStudentStats(testStudent);
        assertEquals(9, finalStats.getTotalClasses());
        assertEquals(5, finalStats.getPresentClasses());
        assertEquals(1, finalStats.getOdClasses());
        assertEquals(75.0, finalStats.getPercentage(), 0.01);
        assertFalse(finalStats.isLowAttendance(), "75.0% is at threshold, not low attendance");
    }
}
