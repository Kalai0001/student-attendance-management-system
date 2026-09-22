package com.college.attendance.controller;

import com.college.attendance.dto.AttendanceStatsDto;
import com.college.attendance.model.Attendance;
import com.college.attendance.model.Student;
import com.college.attendance.service.AttendanceService;
import com.college.attendance.service.StudentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final AttendanceService attendanceService;

    public StudentController(StudentService studentService, AttendanceService attendanceService) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
    }

    private Student getLoggedInStudent(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("No authenticated student user found.");
        }
        return studentService.getStudentByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Student record not linked to username: " + userDetails.getUsername()));
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getLoggedInStudent(userDetails);
        AttendanceStatsDto stats = attendanceService.getStudentStats(student);
        List<Attendance> recentHistory = attendanceService.getStudentHistory(student)
                .stream().limit(6).collect(Collectors.toList());

        model.addAttribute("student", student);
        model.addAttribute("stats", stats);
        model.addAttribute("recentHistory", recentHistory);

        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getLoggedInStudent(userDetails);
        AttendanceStatsDto stats = attendanceService.getStudentStats(student);

        model.addAttribute("student", student);
        model.addAttribute("stats", stats);

        return "student/profile";
    }

    @GetMapping("/attendance")
    public String attendanceHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getLoggedInStudent(userDetails);
        AttendanceStatsDto stats = attendanceService.getStudentStats(student);
        List<Attendance> history = attendanceService.getStudentHistory(student);

        model.addAttribute("student", student);
        model.addAttribute("stats", stats);
        model.addAttribute("history", history);

        return "student/attendance-history";
    }
}
