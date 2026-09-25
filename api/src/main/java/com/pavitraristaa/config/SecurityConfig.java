package com.pavitraristaa.config;

import com.pavitraristaa.auth.security.JwtAuthenticationFilter;
import com.pavitraristaa.common.api.ApiError;
import com.pavitraristaa.common.api.ApiErrorResponse;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
    private final PavitraProperties properties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper, PavitraProperties properties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.properties = properties;
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
                        // Server-to-server, verified by HMAC signature inside the handler itself, not a JWT -
                        // see RazorpayWebhookService.
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/razorpay").permitAll()
                        // The raw HTTP upgrade request can't carry a custom Authorization header from a
                        // browser - real auth happens on the STOMP CONNECT frame instead, once the socket is
                        // open. See StompAuthChannelInterceptor.
                        .requestMatchers("/ws/chat/**").permitAll()
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

    /**
     * Spring Security's .cors(Customizer.withDefaults()) does nothing on its own - it just looks for a
     * CorsConfigurationSource bean, which didn't exist until now. Without this, every cross-origin request
     * from a browser frontend (any real web app, since it won't share this API's origin) was silently blocked
     * by the browser itself before ever reaching a controller. allowCredentials stays false: the JWT travels
     * as an Authorization header, not a cookie, so there's no session credential for the browser to attach.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(java.time.Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
