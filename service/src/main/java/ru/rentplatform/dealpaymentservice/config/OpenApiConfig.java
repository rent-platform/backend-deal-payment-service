package ru.rentplatform.dealpaymentservice.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenAPI dealPaymentServiceOpenAPI(
            @Value("${app.swagger.gateway-url}") String gatewayUrl,
            @Value("${app.swagger.deal-payment-service-url}") String dealPaymentServiceUrl
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("Deal Payment Service API")
                        .description("API для сделок и платежей Rent Platform")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url(gatewayUrl).description("Gateway"),
                        new Server().url(dealPaymentServiceUrl).description("Deal Payment Service (Direct)")
                ));
    }
}
