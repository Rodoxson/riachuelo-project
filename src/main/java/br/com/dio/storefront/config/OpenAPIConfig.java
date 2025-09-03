package br.com.dio.storefront.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenAPIConfig {

    @Bean
    OpenAPI customOpenAPI() {
         return new OpenAPI()
                .info(new Info()
                        .title("Api da vitrine do ecommerce")
                        .description("Documentação da API de controle de estoque")
                        .version("1.0.0"));
    }

}
