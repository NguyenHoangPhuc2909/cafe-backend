package com.example.cafe.config;

import com.example.cafe.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  // 1. Khai bao PasswordEncoder dung thuat toan BCrypt
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // 2. Khai bao AuthencationManager de xu ly dang nhap o tang Auth API
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  // 3. Cau hinh luong bao mat SecurityFilterChain
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.setCharacterEncoding("UTF-8");
              response.getWriter().write("""
                  {"success":false,"message":"Vui lòng đăng nhập lại"}
                  """);
            })
            .accessDeniedHandler((request, response, exception) -> {
              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              response.setContentType("application/json");
              response.setCharacterEncoding("UTF-8");
              response.getWriter().write("""
                  {"success":false,"message":"Bạn không có quyền thực hiện thao tác này"}
                  """);
            }))
        .authorizeHttpRequests(auth -> auth
            // 1. API công khai
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products").permitAll()

            .requestMatchers(HttpMethod.GET, "/api/public/tables/*").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/public/tables/*/access").permitAll()

            .requestMatchers(HttpMethod.GET, "/api/guest/session").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/guest/orders").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/guest/orders").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/guest/orders/*").permitAll()

            // 2. Chỉ admin
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")

            // 3. Chỉ staff
            .requestMatchers(HttpMethod.POST, "/api/staff/orders").hasRole("STAFF")
            .requestMatchers(HttpMethod.POST, "/api/staff/orders/*/payments").hasRole("STAFF")
            .requestMatchers(HttpMethod.PATCH, "/api/staff/orders/*/complete").hasRole("STAFF")
            .requestMatchers("/api/staff/revenue/**").hasRole("STAFF")

            // 4. Dùng chung cho staff và admin
            .requestMatchers(HttpMethod.GET, "/api/orders/*").hasAnyRole("STAFF", "ADMIN")
            .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")
            
            // 5. Các API còn lại yêu cầu đăng nhập
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
