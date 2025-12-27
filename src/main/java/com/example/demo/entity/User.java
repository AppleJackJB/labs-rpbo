package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // ДОБАВЬ ЭТО - обратная связь к UserSession
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSession> sessions = new ArrayList<>();

    // Конструкторы
    public User() {}

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    // ДОБАВЬ ГЕТТЕР И СЕТТЕР ДЛЯ SESSIONS
    public List<UserSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<UserSession> sessions) {
        this.sessions = sessions;
    }

    // Вспомогательные методы
    public void addRole(String role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
    }

    public boolean hasRole(String role) {
        return this.roles != null && this.roles.contains(role);
    }

    // Вспомогательный метод для добавления сессии
    public void addSession(UserSession session) {
        if (this.sessions == null) {
            this.sessions = new ArrayList<>();
        }
        sessions.add(session);
        session.setUser(this);
    }
}