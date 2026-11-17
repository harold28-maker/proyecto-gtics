package pe.edu.pucp.skillbridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena independiente SOLO para /api/identidad/**.
 * No modifica SecurityConfig ni las rutas originales de SkillBridge.
 * Basado en la PPT de Spring Security: HTTP Basic + STATELESS.
 */
@Configuration
public class ApiDniSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiDniSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/identidad/**")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // API autenticada con cabecera Basic, no con cookie de sesion.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasAnyRole("ADMINISTRADOR", "RESOURCE_MANAGER"))
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
