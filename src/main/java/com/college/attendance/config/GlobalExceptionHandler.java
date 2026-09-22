package com.college.attendance.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleGlobalException(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.error("Unhandled application error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        redirectAttributes.addFlashAttribute("errorMessage", 
                "Operation failed: " + (ex.getMessage() != null ? ex.getMessage() : "Unexpected system error occurred."));

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty() && referer.contains("/admin/")) {
            return "redirect:" + referer;
        }

        return "redirect:/admin/dashboard";
    }
}
