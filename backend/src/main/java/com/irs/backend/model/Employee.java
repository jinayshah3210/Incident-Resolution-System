package com.irs.backend.model;

public class Employee extends User {

    private String department;

    public Employee(String name, String email,
                    String passwordHash,
                    String department) {

        super(name, email, passwordHash);
        this.department = department;
    }

    @Override
    public String getRole() {
        return "EMPLOYEE";
    }
}