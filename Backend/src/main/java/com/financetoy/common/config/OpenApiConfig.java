package com.financetoy.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeToyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FinanceToy API")
                .description("금융 코어 시나리오를 작은 범위에서 시뮬레이션하는 REST API")
                .version("v1"));
    }
}
