package com.college.attendance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OtpInterceptor otpInterceptor;

    public WebMvcConfig(OtpInterceptor otpInterceptor) {
        this.otpInterceptor = otpInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(otpInterceptor).addPathPatterns("/student/**");
    }
}
