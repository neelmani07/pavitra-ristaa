package com.pavitraristaa.config;

import com.pavitraristaa.auth.security.JwtAuthenticationFilter;
import com.pavitraristaa.common.api.ApiError;
import com.pavitraristaa.common.api.ApiErrorResponse;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(PavitraProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH = {
            "/api/v1/auth/register",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/resend-otp",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/login",
            "/api/v1/auth/login/otp",
            "/api/v1/auth/oauth/google",
            "/api/v1/auth/oauth/apple",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_AUTH).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/relationship-modes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/master-data/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/reasons").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/safety-center").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/help").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/legal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/plans/**").permitAll()
                        // Baseline for every /admin/** path - defense in depth alongside the finer-grained
                        // @PreAuthorize on individual controller methods (some admin actions are ADMIN/SUPER_ADMIN
                        // only; this just guarantees a plain USER can never reach anything under /admin/**).
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("MODERATOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            Object jwtError = request.getAttribute("jwt_error");
            if (jwtError instanceof ApiException apiException) {
                writeError(response, apiException.getErrorCode(), apiException.getMessage());
                return;
            }
            writeError(response, ErrorCode.UNAUTHORIZED, "Authentication required");
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeError(response, ErrorCode.FORBIDDEN, "Access denied");
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = ApiErrorResponse.of(new ApiError(errorCode.name(), message, null));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
