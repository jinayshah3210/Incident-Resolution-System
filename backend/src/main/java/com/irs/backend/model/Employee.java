
package com.irs.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee extends User {

    private String department;

    protected Employee() {
    }

    public Employee(String name, String email,
                    String passwordHash, String department) {
        super(name, email, passwordHash);
        this.department = department;
    }

    @Override
    public String getRole() {
        return "EMPLOYEE";
    }

    public String getDepartment() {
        return department;
    }
}
