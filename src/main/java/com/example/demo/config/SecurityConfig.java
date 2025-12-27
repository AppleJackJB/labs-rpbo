package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Отключаем CSRF для API (JWT защищает от CSRF)
                .csrf(csrf -> csrf.disable())

                // Настройка авторизации запросов
                .authorizeHttpRequests(auth -> auth
                        // ========== ПУБЛИЧНЫЕ ENDPOINTS (должны быть ПЕРВЫМИ) ==========
                        .requestMatchers(
                                "/api/auth/**",           // Аутентификация
                                "/register",              // Регистрация
                                "/error",                 // Страницы ошибок
                                "/exchange/tls-info",     // TLS демонстрация ✅
                                "/exchange/health"        // Health check ✅
                        ).permitAll()

                        // ========== GET ЗАПРОСЫ (доступны USER и ADMIN) ==========
                        .requestMatchers(HttpMethod.GET,
                                "/accounts/**",           // Аккаунты
                                "/transactions/**",       // Транзакции
                                "/cards/**",              // Карты
                                "/deposits/**",           // Вклады
                                "/loans/**",              // Кредиты
                                "/exchange/rate/**",      // Конкретные курсы валют
                                "/exchange/rates",        // Все курсы валют
                                "/customers/**"           // Клиенты
                        ).hasAnyRole("USER", "ADMIN")

                        // ========== POST ЗАПРОСЫ ДЛЯ USER ==========
                        .requestMatchers(HttpMethod.POST,
                                "/transactions/**",       // Создание транзакций
                                "/deposits/open",         // Открытие вкладов
                                "/loans/take",            // Взятие кредитов
                                "/exchange/convert"       // Конвертация валют
                        ).hasAnyRole("USER", "ADMIN")

                        // ========== POST ЗАПРОСЫ ТОЛЬКО ДЛЯ ADMIN ==========
                        .requestMatchers(HttpMethod.POST,
                                "/accounts/**",           // Создание аккаунтов
                                "/cards/**",              // Создание карт
                                "/customers/**",          // Создание клиентов
                                "/exchange/rate"          // Установка курса валют
                        ).hasRole("ADMIN")

                        // ========== PUT/DELETE ТОЛЬКО ДЛЯ ADMIN ==========
                        .requestMatchers(HttpMethod.PUT, "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")

                        // ========== ADMIN ПАНЕЛЬ ==========
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ========== ВСЕ ОСТАЛЬНЫЕ ЗАПРОСЫ ==========
                        .anyRequest().authenticated()
                )

                // Добавляем JWT фильтр
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Статусные сессии (JWT самодостаточен)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}