package com.college.attendance.controller;

import com.college.attendance.dto.StudentRegistrationDto;
import com.college.attendance.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final StudentService studentService;

    public AuthController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "denied", required = false) String denied,
                            @RequestParam(value = "registered", required = false) String registered,
                            HttpServletRequest request,
                            Authentication authentication,
                            Model model) {
        // If already logged in, redirect to appropriate dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                if (auth.getAuthority().equals("ROLE_ADMIN")) {
                    return "redirect:/admin/dashboard";
                } else if (auth.getAuthority().equals("ROLE_STUDENT")) {
                    return "redirect:/student/dashboard";
                }
            }
        }

        if (error != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Exception ex = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                if (ex instanceof DisabledException) {
                    model.addAttribute("errorMessage", "Your account is PENDING Admin approval. Please wait for an administrator to activate it.");
                } else {
                    model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
                }
            } else {
                model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
            }
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (denied != null) {
            model.addAttribute("errorMessage", "Access Denied: You do not have permission to view that page.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created successfully! Your registration is PENDING Admin approval before you can sign in.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                if (auth.getAuthority().equals("ROLE_ADMIN")) {
                    return "redirect:/admin/dashboard";
                } else if (auth.getAuthority().equals("ROLE_STUDENT")) {
                    return "redirect:/student/dashboard";
                }
            }
        }
        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new StudentRegistrationDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationDto") StudentRegistrationDto registrationDto,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            studentService.registerStudent(registrationDto);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Account created successfully! Your registration is PENDING Admin approval. You will be able to log in once an administrator approves your account.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "An unexpected error occurred during registration. Please try again.");
            return "register";
        }
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                if (auth.getAuthority().equals("ROLE_ADMIN")) {
                    return "redirect:/admin/dashboard";
                } else if (auth.getAuthority().equals("ROLE_STUDENT")) {
                    return "redirect:/student/dashboard";
                }
            }
        }
        return "redirect:/login";
    }
}
