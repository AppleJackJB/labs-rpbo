package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RegistrationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");

    public Map<String, Object> registerUser(String username, String password, String email) {
        // Проверка существования
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Проверка пароля
        validatePassword(password);

        // Создание пользователя
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);

        Set<String> roles = new HashSet<>();
        roles.add("USER");
        user.setRoles(roles);

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", username);
        response.put("roles", user.getRoles());

        return response;
    }

    public Map<String, Object> registerAdmin(String username, String password, String email) {
        // Проверка существования
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Проверка пароля
        validatePassword(password);

        // Создание пользователя с ролью ADMIN
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);

        Set<String> roles = new HashSet<>();
        roles.add("USER");
        roles.add("ADMIN");
        user.setRoles(roles);

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin registered successfully");
        response.put("username", username);
        response.put("roles", user.getRoles());

        return response;
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must contain: at least one uppercase letter, " +
                            "one lowercase letter, one digit, and one special character (@#$%^&+=)"
            );
        }
    }

    public boolean isPasswordValid(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}