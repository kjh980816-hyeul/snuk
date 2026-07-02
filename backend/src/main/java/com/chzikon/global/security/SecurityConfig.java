package com.chzikon.global.security;

import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // OAuth 시작/콜백
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // 공개 조회 (홈)
                        .requestMatchers(HttpMethod.GET,
                                "/api/campaigns", "/api/campaigns/*",
                                "/api/campaigns/*/reviews", "/api/reviews",
                                "/api/collab/**",
                                "/api/tournaments", "/api/tournaments/*",
                                "/api/goods", "/api/goods/*").permitAll()
                        // PG 결제 웹훅(서버-서버, JWT 없음) — 서버가 재조회로 검증
                        .requestMatchers("/api/payments/webhook").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
                        // 어드민
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 나머지 /api/** 는 로그인 필요
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, e) -> write(res, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) -> write(res, ErrorCode.FORBIDDEN)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        // H2 콘솔 프레임 허용 (local)
        http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    private void write(jakarta.servlet.http.HttpServletResponse res, ErrorCode ec) throws java.io.IOException {
        res.setStatus(ec.getStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ErrorResponse.of(ec));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
