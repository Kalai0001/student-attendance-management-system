package com.college.attendance.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

import com.college.attendance.service.OtpService;
import com.college.attendance.service.EmailService;
import com.college.attendance.service.StudentService;
import com.college.attendance.model.Student;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OtpService otpService;
    private final EmailService emailService;
    private final StudentService studentService;

    public CustomAuthenticationSuccessHandler(OtpService otpService, 
                                              EmailService emailService, 
                                              StudentService studentService) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.studentService = studentService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            } else if (role.equals("ROLE_STUDENT")) {
                String username = authentication.getName();
                
                // Fetch student's email
                Student student = studentService.getStudentByUsername(username).orElse(null);
                
                if (student != null && student.isEmailVerified()) {
                    request.getSession().setAttribute("otpVerified", true);
                    response.sendRedirect(request.getContextPath() + "/student/dashboard");
                    return;
                }

                if (student != null && student.getEmail() != null) {
                    String otp = otpService.generateOtp(username);
                    emailService.sendOtpEmail(student.getEmail(), otp);
                }
                
                // Set session attribute indicating OTP is required
                request.getSession().setAttribute("otpVerified", false);
                
                response.sendRedirect(request.getContextPath() + "/verify-otp");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/login");
    }
}
