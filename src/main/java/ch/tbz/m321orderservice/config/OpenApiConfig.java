package ch.tbz.m321orderservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI orderServiceOpenApi(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return new OpenAPI()
                .info(new Info()
                        .title("M321 Order Service API")
                        .version("5.0.0")
                        .description("Order intake API with parallel v1/v2 migration and asynchronous RabbitMQ processing"))
                .components(new Components()
                        .addSecuritySchemes("bearerJwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Keycloak OAuth2 access token"))
                        .addSecuritySchemes("keycloakOidc", new SecurityScheme()
                                .type(SecurityScheme.Type.OPENIDCONNECT)
                                .openIdConnectUrl(issuerUri + "/.well-known/openid-configuration")
                                .description("Keycloak OpenID Connect discovery")))
                .addSecurityItem(new SecurityRequirement().addList("bearerJwt"))
                .addSecurityItem(new SecurityRequirement().addList("keycloakOidc"));
    }
}
