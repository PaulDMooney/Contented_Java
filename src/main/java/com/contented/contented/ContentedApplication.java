package com.contented.contented;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Hooks;

import java.time.Clock;

@SpringBootApplication(exclude = {LogbackMetricsAutoConfiguration.class, SqlInitializationAutoConfiguration.class})
@OpenAPIDefinition(info = @Info(title = "Contented API", version = "1.0", description = "Documentation Contented API v1.0"))
public class ContentedApplication {

	public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(ContentedApplication.class, args);
	}

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

}
