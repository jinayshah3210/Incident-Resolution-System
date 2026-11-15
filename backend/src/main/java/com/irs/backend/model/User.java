package com.irs.backend.model;

public abstract class User {

    protected Long id;
    protected String name;
    protected String email;
    protected String passwordHash;
    protected boolean active;

    public User(String name, String email,
                String passwordHash) {

        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
    }

    public abstract String getRole();

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}