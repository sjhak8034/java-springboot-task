package com.example.javaspringboottask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JavaSpringbootTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaSpringbootTaskApplication.class, args);
    }

}
