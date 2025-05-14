package com.example.javaspringboottask.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 보안 속성을 관리하는 클래스. application.yml의 security 속성과 매핑된다.
 */
@ConfigurationProperties(prefix = "security")
@Component
@Getter
@Setter
public class SecurityProperties {

    private List<String> whiteList = new ArrayList<>();  // 인증 없이 접근 가능한 경로 목록
    private List<String> userAuthList = new ArrayList<>(); // 판매자 권한이 필요한 경로 목록
    private List<String> adminAuthList = new ArrayList<>(); // 관리자 권한이 필요한 경로 목록
    private Map<HttpMethod, List<String>> methodSpecificPatterns = new HashMap<>(); // 특정 HTTP 메서드에 대한 보안 패턴
}
