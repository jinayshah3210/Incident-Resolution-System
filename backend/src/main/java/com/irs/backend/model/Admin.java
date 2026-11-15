package com.irs.backend.model;

public class Admin extends User {

    private String department;

    public Admin(String name, String email,
                    String passwordHash,
                    String department) {

        super(name, email, passwordHash);
        this.department = department;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}