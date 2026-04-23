package tw.com.stockplatform.boot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tw.com.stockplatform.boot.config.security.ApiAccessDeniedHandler;
import tw.com.stockplatform.boot.config.security.ApiAuthenticationEntryPoint;
import tw.com.stockplatform.boot.config.security.JwtAuthenticationFilter;

import java.util.List;

/**
 * Wave 3 F-W3-04-5 / Q4 拍板：Spring Security 主配置（放 stock-boot）。
 * <p>
 * 設計原則：
 * <ol>
 *   <li>Envelope Pattern 不破壞：未授權一律 HTTP 200 + code 3001/3002/3003（D-08）</li>
 *   <li>Wave 2 既有 4 個公開端點（quote/fundamental/chip）完全不受衝擊</li>
 *   <li>CSRF 關閉：SPA + Bearer Token 架構，無 cookie session（refresh token 走 HttpOnly cookie，
 *       但 refresh endpoint 限制 SameSite=Lax 豁免）</li>
 *   <li>SessionCreationPolicy.STATELESS：不建 HttpSession</li>
 *   <li>CORS 嚴格限制：local=localhost:5173，dev/prod 各自網域，禁止 *</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(apiAuthenticationEntryPoint)
                .accessDeniedHandler(apiAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Wave 2 既有公開端點（quote / fundamental / chip），不受 Security 衝擊
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/quote/get",
                    "/api/v1/quote/list",
                    "/api/v1/quote/history",
                    "/api/v1/fundamental/get",
                    "/api/v1/chip/get"
                ).permitAll()

                // 認證相關（登入 / 註冊 / refresh 不需 token）
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/member/login",
                    "/api/v1/member/register",
                    "/api/v1/member/refresh",
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/verify-email",
                    "/api/v1/auth/resend-verification"
                ).permitAll()

                // Wave 3 搜尋公開端點（搜尋 / 熱門搜尋允許未登入）
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/search/stock",
                    "/api/v1/search/popular",
                    // SRS 定義的路徑（兩種 path 皆許可，以免前後端對齊期間出問題）
                    "/api/v1/stock/search",
                    "/api/v1/stock/hot-search"
                ).permitAll()

                // Actuator 健康 / 資訊端點
                .requestMatchers(HttpMethod.GET,
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info"
                ).permitAll()

                // OpenAPI / Swagger（local/dev/uat 開放；prod 由 application-prod.yml 關閉 springdoc）
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // 其餘 /api/v1/** 一律需要認證
                .requestMatchers("/api/v1/**").authenticated()

                // 預設拒絕（Deny by default）
                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(h -> h
                // X-Content-Type-Options: nosniff
                .contentTypeOptions(c -> {})
                // X-Frame-Options: DENY（防 clickjacking）
                .frameOptions(f -> f.deny())
                // HSTS
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000))
                // Referrer-Policy
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );

        return http.build();
    }

    /**
     * CORS 設定。
     * <p>
     * 各環境允許的 origin 分別為：
     * <ul>
     *   <li>local：http://localhost:5173（Vite 預設 port）</li>
     *   <li>dev：https://dev-stock.example.com</li>
     *   <li>prod：https://stock.example.com</li>
     * </ul>
     * 禁止使用萬用字元 * （security-owasp.md A01）。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "https://dev-stock.example.com",
            "https://stock.example.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Trace-Id",
            "X-Request-Id"
        ));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
