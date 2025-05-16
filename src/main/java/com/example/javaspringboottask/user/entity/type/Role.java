package com.example.javaspringboottask.user.entity.type;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Schema(description = "사용자 권한 역할")
public enum Role {

    @Schema(description = "일반 사용자 권한")
    USER("user"),

    @Schema(description = "관리자 권한")
    ADMIN("admin");

    private final String name;

    /**
     * 문자열로부터 Role enum 객체를 생성
     */
    public static Role of(String roleName) throws IllegalArgumentException {
        for (Role role : values()) {
            if (role.getName().equals(roleName.toLowerCase())) {
                return role;
            }
        }
        throw new IllegalArgumentException("해당하는 이름의 권한을 찾을 수 없습니다: " + roleName);
    }

    /**
     * 스프링 시큐리티 권한 부여용
     */
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.name()));
    }
}