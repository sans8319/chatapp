package com.chatapp.chatservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Path ko absolute path mein convert karte hain taaki OS confuse na ho
        Path uploadPath = Paths.get(uploadDir);
        String absPath = uploadPath.toFile().getAbsolutePath();
        
        // Browser mein agar '/uploads/...' likha ho, toh wo system ke folder mein dhunde
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absPath + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Frontend (Angular) ko backend se file upload/access karne ki permission dena
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}