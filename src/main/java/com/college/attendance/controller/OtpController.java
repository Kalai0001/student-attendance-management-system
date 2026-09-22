package com.college.attendance.controller;

import com.college.attendance.model.Student;
import com.college.attendance.service.EmailService;
import com.college.attendance.service.OtpService;
import com.college.attendance.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OtpController {

    private final OtpService otpService;
    private final EmailService emailService;
    private final StudentService studentService;

    public OtpController(OtpService otpService, EmailService emailService, StudentService studentService) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.studentService = studentService;
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isStudent = false;
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (auth.getAuthority().equals("ROLE_STUDENT")) {
                isStudent = true;
                break;
            }
        }

        if (!isStudent) {
            return "redirect:/admin/dashboard";
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");
            if (Boolean.TRUE.equals(otpVerified)) {
                return "redirect:/student/dashboard";
            }
        }
        
        // Mask email for display
        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        if (student != null && student.getEmail() != null) {
            String email = student.getEmail();
            int atIndex = email.indexOf("@");
            if (atIndex > 1) {
                String masked = email.charAt(0) + "***" + email.substring(atIndex - 1);
                model.addAttribute("maskedEmail", masked);
            } else {
                model.addAttribute("maskedEmail", email);
            }
        }

        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String otp,
                            Authentication authentication,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        boolean isValid = otpService.validateOtp(username, otp);

        if (isValid) {
            studentService.markEmailAsVerified(username);
            request.getSession().setAttribute("otpVerified", true);
            return "redirect:/student/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired OTP. Please try again.");
            return "redirect:/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        
        if (student != null && student.getEmail() != null) {
            String newOtp = otpService.generateOtp(username);
            emailService.sendOtpEmail(student.getEmail(), newOtp);
            redirectAttributes.addFlashAttribute("successMessage", "A new OTP has been sent to your registered email.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not find registered email address.");
        }

        return "redirect:/verify-otp";
    }
}
