package com.josalukkas.digigold.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jos Alukkas DigiGold API")
                        .description("""
                                REST API backend for the Jos Alukkas DigiGold platform.
                                
                                **Features**
                                - JWT authentication (access + refresh tokens)
                                - Live gold price management
                                - Buy gold via Razorpay (demo)
                                - Sell gold with instant wallet settlement
                                - Wallet & portfolio tracking
                                - Admin panel for users and price management
                                
                                **Authentication**
                                
                                Use `POST /auth/login` to obtain a Bearer token, then click
                                **Authorize** and paste the token to unlock protected endpoints.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Jos Alukkas DigiGold")
                                .email("support@josalukkas.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token")));
    }
}
