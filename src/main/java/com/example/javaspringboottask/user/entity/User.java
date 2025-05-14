package com.example.javaspringboottask.user.entity;

import com.example.javaspringboottask.global.entity.BaseTimeEntity;
import com.example.javaspringboottask.user.entity.type.Role;
import jakarta.persistence.*;
import lombok.Getter;
@Getter
@Entity
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String password;

    @Enumerated(value = EnumType.STRING)
    private Role role;

    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public User() {

    }

}