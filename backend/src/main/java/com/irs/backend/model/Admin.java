
package com.irs.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends User {

    private String department;

    protected Admin() {
    }

    public Admin(String name, String email,
                 String passwordHash, String department) {
        super(name, email, passwordHash);
        this.department = department;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    public String getDepartment() {
        return department;
    }
}
