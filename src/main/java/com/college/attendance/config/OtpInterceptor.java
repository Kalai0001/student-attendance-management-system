package com.college.attendance.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OtpInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            boolean isStudent = false;
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority().equals("ROLE_STUDENT")) {
                    isStudent = true;
                    break;
                }
            }

            if (isStudent) {
                HttpSession session = request.getSession(false);
                Boolean otpVerified = (session != null) ? (Boolean) session.getAttribute("otpVerified") : null;

                if (otpVerified == null || !otpVerified) {
                    response.sendRedirect(request.getContextPath() + "/verify-otp");
                    return false;
                }
            }
        }
        return true;
    }
}
