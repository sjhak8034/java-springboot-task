package com.example.javaspringboottask.user.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER("user"),
    ADMIN("admin");

    private final String name;

    //User 엔티티를 생성할 때 사용
    public static Role of(String roleName) throws IllegalArgumentException {
        for (Role role : values()) {
            if (role.getName().equals(roleName.toLowerCase())) {
                return role;
            }
        }

        throw new IllegalArgumentException("해당하는 이름의 권한을 찾을 수 없습니다: " + roleName);
    }

    /* Spring Security는 권한을 확인할 때, GrantedAuthority 객체의 getAuthority() 값과 비교함.
     * 또 스프링 시큐리티의 FilterChain 에서 hasRole 메서드는 내부적으로 ROLE_ 접두사를 자동적으로 추가함.
     * "USER" 를 넣어도 ROLE_USER 와 비교하게 된다는 뜻.
     * 따라서 getAuthorities 를 할 때 생성되는 GrantedAuthority 값을 ROLE_ 을 붙여줘야함.*/

    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.name()));
    }
}

