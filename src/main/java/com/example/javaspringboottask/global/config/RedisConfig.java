package com.example.javaspringboottask.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    /**
     * Redis 서버와 애플리케이션 간 연결을 설정하고 관리, LettuceConnectionFactory 사용
     *
     * @return 연결 팩토리를 생성
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        //LettuceConnectionFactory 는 Lettuce 클라이언트를 사용하여 연결 팩토리를 생성해주는 역할
        //호스트와 포트 정보를 사용하여 Redis 서버와의 연결 설정을 해줌.
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * Redis 와의 데이터 입출력을 위한 주요 인터페이스 RedisTemplate Key-Value 구조로 데이터를 저장, 조회, 삭제 등의 작업을 수행하는 역할
     * 직렬화,역직렬화 설정을 통해 데이터 형식을 관리할 수 있음
     *
     * @return 설정된 template 리턴
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        // Redis에 저장할 Key를 String 형태로 직렬화
        template.setKeySerializer(new StringRedisSerializer());

        // Redis에 저장할 Value를 String 형태로 직렬화
        template.setValueSerializer(new StringRedisSerializer());

        // Redis 연결 팩토리 설정
        template.setConnectionFactory(redisConnectionFactory());

        return template;
    }

    /**
     * string-object 저장을 위한 redis template
     *
     * @return 설정된 template 리턴
     */
    @Bean
    public RedisTemplate<String, Object> objectRedisTemplate() {

        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(redisConnectionFactory());

        template.setKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }



}
