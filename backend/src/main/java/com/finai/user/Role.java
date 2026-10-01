package com.finai.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Role is a table rather than an enum so that role assignment is data, not code.
 */
@Entity
@Table(name = "roles")
public class Role {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 32)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    protected Role() {
        // for JPA
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
