package io.github.kcbear.trading.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Purpose: CORS configuration so a front end served from another origin (e.g. a Vite dev
 * server without proxy) can call /api and read the Content-Range header that carries the
 * total record count for react-admin paging.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .exposedHeaders("Content-Range");
    }
}
