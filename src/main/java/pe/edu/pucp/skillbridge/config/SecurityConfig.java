package pe.edu.pucp.skillbridge.config;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import pe.edu.pucp.skillbridge.controller.LoginSuccessHandler;

/**
 * Configura la seguridad web de SkillBridge, incluyendo el cifrado de contraseñas,
 * las reglas de acceso por ruta, el inicio de sesión por formulario y el cierre
 * de sesión.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * Crea el codificador utilizado para almacenar y verificar contraseñas mediante
     * el algoritmo BCrypt.
     *
     * @return instancia de PasswordEncoder basada en BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
    /**
     * Configura el acceso a rutas web, el inicio de sesión por formulario, la
     * redirección de éxito y el cierre de sesión de la aplicación.
     *
     * @param http configuración de seguridad HTTP proporcionada por Spring
     * @param successHandler manejador ejecutado cuando la autenticación finaliza correctamente
     * @param sessionRegistry registro de sesiones para invalidarlas al restablecer la contraseña
     * @param validator validador de los campos del formulario de acceso
     * @return cadena de filtros configurada para las rutas de la aplicación
     * @throws Exception si Spring Security no puede construir la cadena de filtros
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginSuccessHandler successHandler, SessionRegistry sessionRegistry,
                                           Validator validator) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/sesion/portada", "/css/**", "/js/**",
                                "/images/**", "/assets/**", "/favicon.ico",
                                "/recuperar-contrasena", "/restablecer-contrasena").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/pm/**").hasRole("PROJECT_MANAGER")
                        .requestMatchers("/resource/**").hasRole("RESOURCE_MANAGER")
                        .requestMatchers("/colaborador/**").hasRole("COLABORADOR")
                        .requestMatchers("/foros/**").hasAnyRole("COLABORADOR", "PROJECT_MANAGER")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login"))
                .addFilterBefore(new ValidacionLoginFilter(validator), UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/acceder")
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .defaultLogoutSuccessHandlerFor(
                                new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT),
                                new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"))
                        .deleteCookies("JSESSIONID", "SESSION")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );
        return http.build();
    }
}
