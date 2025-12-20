package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    /*
    // Старая настройка CSRF (закомментировано)
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName("_csrf");
    */

        http
                // ВРЕМЕННО ОТКЛЮЧАЕМ CSRF для тестирования
                .csrf(csrf -> csrf.disable())

                /*
                // Старый код CSRF (закомментировано)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                */

                .authorizeHttpRequests(auth -> auth
                        // Публичные endpoints (без аутентификации)
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/register").permitAll()
                        .requestMatchers("/error").permitAll()

                        // USER endpoints (только чтение)
                        .requestMatchers(HttpMethod.GET,
                                "/accounts/**",
                                "/transactions/**",
                                "/cards/**",
                                "/deposits/**",
                                "/loans/**",
                                "/exchange/**").hasAnyRole("USER", "ADMIN")

                        // USER операции (создание/действия)
                        .requestMatchers(HttpMethod.POST,
                                "/transactions/**",
                                "/deposits/open",
                                "/loans/take",
                                "/exchange/convert").hasAnyRole("USER", "ADMIN")

                        // ADMIN только endpoints (полный доступ)
                        .requestMatchers(HttpMethod.POST,
                                "/accounts/**",
                                "/cards/**",
                                "/customers/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}