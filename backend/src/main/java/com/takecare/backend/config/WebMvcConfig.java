package com.takecare.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.takecare.backend.user.security.ClinicalDocAccessInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ClinicalDocAccessInterceptor clinicalDocAccessInterceptor;

    public WebMvcConfig(ClinicalDocAccessInterceptor clinicalDocAccessInterceptor) {
        this.clinicalDocAccessInterceptor = clinicalDocAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clinicalDocAccessInterceptor)
                .addPathPatterns("/api/v1/patients/clinical-docs/**");
    }
}
