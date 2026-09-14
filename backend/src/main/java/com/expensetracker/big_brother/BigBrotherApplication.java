package com.expensetracker.big_brother;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Big Brother API", version = "v1"))
@SpringBootApplication
public class BigBrotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigBrotherApplication.class, args);
    }

}
