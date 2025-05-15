package com.example.javaspringboottask.user.entity;

import com.example.javaspringboottask.global.entity.BaseTimeEntity;
import com.example.javaspringboottask.global.valid.ValidPassword;
import com.example.javaspringboottask.user.entity.type.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Length(min = 5, max = 20)
    private String username;

    @Column(nullable = false)
    @ValidPassword
    private String password;

    @Column(nullable = false)
    @Length(min = 5, max = 20)
    private String nickname;

    @Enumerated(value = EnumType.STRING)
    private Role role = Role.USER;

    public User(String username, String nickname, String password) {
        this.username = username;
        this.nickname = nickname;
        this.password = password;
    }


    public void grantAdmin(){
        this.role = Role.ADMIN;
    }

    // 테스트용 생성자 입니다
    public User(String username, String nickname, String password, Role role) {
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.role = role;
    }

}