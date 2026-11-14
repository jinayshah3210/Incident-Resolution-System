package com.irs.backend.dto;

public class LoginResponse {
    private String token;
    private UserInfo user;

    public LoginResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserInfo getUser() {
        return user;
    }

    public static class UserInfo {
        private Long id;
        private String name;
        private String role;

        public UserInfo(Long id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }
    }
}