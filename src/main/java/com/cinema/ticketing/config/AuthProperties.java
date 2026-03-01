package com.cinema.ticketing.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private List<UserCredential> users = new ArrayList<>();

    public Jwt getJwt() {
        return jwt;
    }

    public List<UserCredential> getUsers() {
        return users;
    }

    public void setUsers(List<UserCredential> users) {
        this.users = users;
    }

    public static class Jwt {
        @NotBlank
        private String secret;
        private long expirationMinutes = 120;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    public static class UserCredential {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        @NotBlank
        private String role;

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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
