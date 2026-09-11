package com.ibizabroker.bibliotheque.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS global : les origines autorisées ne sont pas codées en dur,
 * elles viennent de la propriété app.cors.allowed-origins
 * (variable d'environnement APP_CORS_ALLOWED_ORIGINS, origines séparées par des virgules).
 * PATCH est autorisé pour les annulations de réservation.
 */
@Configuration
public class CorsConfiguration {

    private static final String[] METHODES_AUTORISEES = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};

    private final String[] originesAutorisees;

    public CorsConfiguration(@Value("${app.cors.allowed-origins}") List<String> originesAutorisees) {
        this.originesAutorisees = originesAutorisees.toArray(new String[0]);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods(METHODES_AUTORISEES)
                        .allowedHeaders("*")
                        .allowedOrigins(originesAutorisees)
                        .allowCredentials(true);
            }
        };
    }
}