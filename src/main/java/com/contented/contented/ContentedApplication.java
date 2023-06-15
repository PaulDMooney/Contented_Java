package com.contented.contented;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Contented API", version = "1.0", description = "Documentation Contented API v1.0"))
public class ContentedApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentedApplication.class, args);
	}

}
