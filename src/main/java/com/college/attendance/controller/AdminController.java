package com.college.attendance.controller;

import com.college.attendance.dto.AttendanceSheetForm;
import com.college.attendance.dto.AttendanceStatsDto;
import com.college.attendance.dto.StudentForm;
import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceStatus;
import com.college.attendance.model.Student;
import com.college.attendance.service.AttendanceService;
import com.college.attendance.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StudentService studentService;
    private final AttendanceService attendanceService;

    public AdminController(StudentService studentService, AttendanceService attendanceService) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
    }

    // ==================== DASHBOARD ====================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> summary = attendanceService.getDashboardSummary();
        model.addAllAttributes(summary);
        // Recent 10 student registrations (real data, most recently added first)
        List<Student> recentStudents = studentService.getRecentStudents();
        model.addAttribute("recentStudents", recentStudents);
        return "admin/dashboard";
    }

    // ==================== STUDENT MANAGEMENT ====================
    @GetMapping("/students")
    public String listStudents(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "dept", required = false) String dept,
                               @RequestParam(value = "year", required = false) Integer year,
                               @RequestParam(value = "sec", required = false) String sec,
                               Model model) {
        List<Student> students = studentService.searchStudents(keyword, dept, year, sec);
        List<AttendanceStatsDto> studentStats = new ArrayList<>();
        for (Student s : students) {
            studentStats.add(attendanceService.getStudentStats(s));
        }

        model.addAttribute("students", studentStats);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDept", dept);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedSec", sec);
        model.addAttribute("departments", studentService.getDistinctDepartments());
        model.addAttribute("years", studentService.getDistinctYears());
        model.addAttribute("sections", studentService.getDistinctSections());

        return "admin/students";
    }

    @GetMapping("/students/new")
    public String newStudentForm(Model model) {
        model.addAttribute("studentForm", new StudentForm());
        model.addAttribute("isEdit", false);
        model.addAttribute("departments", studentService.getDistinctDepartments());
        model.addAttribute("years", studentService.getDistinctYears());
        model.addAttribute("sections", studentService.getDistinctSections());
        return "admin/student-form";
    }

    @PostMapping("/students/save")
    public String saveStudent(@Valid @ModelAttribute("studentForm") StudentForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (form.getPassword() == null || form.getPassword().trim().isEmpty()) {
            form.setPassword("student123");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("departments", studentService.getDistinctDepartments());
            model.addAttribute("years", studentService.getDistinctYears());
            model.addAttribute("sections", studentService.getDistinctSections());
            return "admin/student-form";
        }

        try {
            studentService.createStudent(form);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Student " + form.getName() + " (" + form.getRegisterNumber() + ") added successfully!");
            return "redirect:/admin/students";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("departments", studentService.getDistinctDepartments());
            model.addAttribute("years", studentService.getDistinctYears());
            model.addAttribute("sections", studentService.getDistinctSections());
            return "admin/student-form";
        }
    }

    @GetMapping("/students/edit/{id}")
    public String editStudentForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return studentService.getStudentById(id).map(student -> {
            model.addAttribute("studentForm", studentService.toForm(student));
            model.addAttribute("isEdit", true);
            model.addAttribute("departments", studentService.getDistinctDepartments());
            model.addAttribute("years", studentService.getDistinctYears());
            model.addAttribute("sections", studentService.getDistinctSections());
            return "admin/student-form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Student not found with ID: " + id);
            return "redirect:/admin/students";
        });
    }

    @PostMapping("/students/update/{id}")
    public String updateStudent(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("studentForm") StudentForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("departments", studentService.getDistinctDepartments());
            model.addAttribute("years", studentService.getDistinctYears());
            model.addAttribute("sections", studentService.getDistinctSections());
            return "admin/student-form";
        }

        try {
            studentService.updateStudent(id, form);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Student details for " + form.getName() + " updated successfully!");
            return "redirect:/admin/students";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("departments", studentService.getDistinctDepartments());
            model.addAttribute("years", studentService.getDistinctYears());
            model.addAttribute("sections", studentService.getDistinctSections());
            return "admin/student-form";
        }
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.deleteStudent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student and attendance records deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete student: " + ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/approve/{id}")
    public String approveStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.approveStudent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student account approved successfully. The student can now sign in.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not approve student: " + ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/reject/{id}")
    public String rejectStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.rejectStudent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pending student registration has been rejected and removed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not reject student: " + ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    // ==================== MARK ATTENDANCE ====================
    @GetMapping("/attendance/mark")
    public String markAttendanceForm(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(value = "period", required = false, defaultValue = "1") Integer period,
                                     @RequestParam(value = "subject", required = false, defaultValue = "Data Structures") String subject,
                                     @RequestParam(value = "dept", required = false) String dept,
                                     @RequestParam(value = "year", required = false) Integer year,
                                     @RequestParam(value = "sec", required = false) String sec,
                                     Model model) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        Integer targetPeriod = (period != null && period >= 1 && period <= 8) ? period : 1;
        String targetSubject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "Data Structures";

        AttendanceSheetForm sheetForm = attendanceService.prepareSheetForClass(targetDate, targetPeriod, targetSubject, dept, year, sec);

        model.addAttribute("sheetForm", sheetForm);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("selectedPeriod", targetPeriod);
        model.addAttribute("selectedSubject", targetSubject);
        model.addAttribute("selectedDept", dept);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedSec", sec);
        model.addAttribute("departments", studentService.getDistinctDepartments());
        model.addAttribute("years", studentService.getDistinctYears());
        model.addAttribute("sections", studentService.getDistinctSections());

        return "admin/mark-attendance";
    }

    @PostMapping("/attendance/save")
    public String saveAttendanceSheet(@ModelAttribute("sheetForm") AttendanceSheetForm sheetForm,
                                      RedirectAttributes redirectAttributes) {
        LocalDate date = sheetForm.getDate() != null ? sheetForm.getDate() : LocalDate.now();
        Integer period = (sheetForm.getPeriod() != null && sheetForm.getPeriod() >= 1 && sheetForm.getPeriod() <= 8) ? sheetForm.getPeriod() : 1;
        String subject = (sheetForm.getSubject() != null && !sheetForm.getSubject().trim().isEmpty()) ? sheetForm.getSubject().trim() : "General";

        sheetForm.setDate(date);
        sheetForm.setPeriod(period);
        sheetForm.setSubject(subject);

        try {
            int count = attendanceService.saveAttendanceSheet(sheetForm);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Successfully recorded attendance for " + count + " students in Period " + period + " (" + subject + ") on " + date + "!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Could not save attendance: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown database error."));
        }
        
        String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8);
        return "redirect:/admin/attendance/mark?date=" + date + 
                "&period=" + period +
                "&subject=" + encodedSubject +
                (sheetForm.getDepartment() != null && !sheetForm.getDepartment().isEmpty() ? "&dept=" + sheetForm.getDepartment() : "") +
                (sheetForm.getYear() != null ? "&year=" + sheetForm.getYear() : "") +
                (sheetForm.getSection() != null && !sheetForm.getSection().isEmpty() ? "&sec=" + sheetForm.getSection() : "");
    }

    // ==================== ATTENDANCE HISTORY ====================
    @GetMapping("/attendance/history")
    public String attendanceHistory(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(value = "period", required = false) Integer period,
                                    @RequestParam(value = "subject", required = false) String subject,
                                    @RequestParam(value = "dept", required = false) String dept,
                                    @RequestParam(value = "status", required = false) AttendanceStatus status,
                                    @RequestParam(value = "keyword", required = false) String keyword,
                                    Model model) {
        List<Attendance> history = attendanceService.searchAttendanceHistory(date, period, subject, dept, status, keyword);

        long presentCount = history.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long odCount = history.stream().filter(a -> a.getStatus() == AttendanceStatus.OD).count();
        long absentCount = history.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long leaveGrantedCount = history.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE_GRANTED).count();
        long leaveDeniedCount = history.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE_DENIED).count();

        model.addAttribute("history", history);
        model.addAttribute("totalCount", history.size());
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("odCount", odCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("leaveGrantedCount", leaveGrantedCount);
        model.addAttribute("leaveDeniedCount", leaveDeniedCount);

        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedDept", dept);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);

        model.addAttribute("departments", studentService.getDistinctDepartments());
        model.addAttribute("statuses", AttendanceStatus.values());

        return "admin/attendance-history";
    }

    // ==================== ATTENDANCE REPORT ====================
    @GetMapping("/attendance/report")
    public String attendanceReport(@RequestParam(value = "dept", required = false) String dept,
                                   @RequestParam(value = "year", required = false) Integer year,
                                   @RequestParam(value = "sec", required = false) String sec,
                                   Model model) {
        List<AttendanceStatsDto> reportList = attendanceService.getAllStudentsStats(dept, year, sec);

        long lowAttendanceCount = reportList.stream().filter(AttendanceStatsDto::isLowAttendance).count();

        model.addAttribute("reportList", reportList);
        model.addAttribute("totalStudents", reportList.size());
        model.addAttribute("lowAttendanceCount", lowAttendanceCount);
        model.addAttribute("selectedDept", dept);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedSec", sec);
        model.addAttribute("departments", studentService.getDistinctDepartments());
        model.addAttribute("years", studentService.getDistinctYears());
        model.addAttribute("sections", studentService.getDistinctSections());

        return "admin/attendance-report";
    }
}
