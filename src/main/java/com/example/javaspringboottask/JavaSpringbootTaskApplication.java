package com.example.javaspringboottask;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@OpenAPIDefinition(
        info = @Info(title = "My API", version = "1.0", description = "User management API")
)
@SpringBootApplication
@EnableJpaAuditing
public class JavaSpringbootTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaSpringbootTaskApplication.class, args);
    }

}
