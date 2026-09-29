package com.nutriscan;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth;

    public WebConfig(AuthInterceptor auth) { this.auth = auth; }

    @Override
    public void addInterceptors(InterceptorRegistry r) {
        r.addInterceptor(auth).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry r) {
        Path dir = Paths.get("uploads").toAbsolutePath();
        try { Files.createDirectories(dir); } catch (Exception ignored) { }
        r.addResourceHandler("/uploads/**").addResourceLocations(dir.toUri().toString());
    }
}
