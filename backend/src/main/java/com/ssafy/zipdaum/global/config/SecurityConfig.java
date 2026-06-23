package com.ssafy.zipdaum.global.config;

import com.ssafy.zipdaum.global.security.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;

  @Value("${app.cors.allowed-origin-patterns:}")
  private String allowedOriginPatterns;

  @Bean
  public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/properties/compare/ai").hasRole("USER")
            .requestMatchers("/properties/recommendations").hasRole("USER")
            .requestMatchers("/properties/*/recommendation-score").hasRole("USER")
            .requestMatchers("/properties/*/interactions").hasRole("USER")
            .requestMatchers("/users/info", "/users/info/**").hasRole("USER")
                .requestMatchers("/api/admin/batch/**").hasRole("ADMIN")
            .anyRequest().permitAll()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(parseAllowedOriginPatterns());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private List<String> parseAllowedOriginPatterns() {
    return parseCorsValues(String.join(",", allowedOrigins, allowedOriginPatterns));
  }

  private List<String> parseCorsValues(String corsValues) {
    return Arrays.stream(corsValues.split(","))
        .map(String::trim)
        .map(origin -> origin.replaceAll("^['\"]|['\"]$", ""))
        .filter(origin -> !origin.isBlank())
        .toList();
  }
}
