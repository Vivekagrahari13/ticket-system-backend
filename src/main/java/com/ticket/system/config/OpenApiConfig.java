package com.ticket.system.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Ticket System API", version = "1.0",
                description = "Helpdesk API: users raise tickets, admins manage them."),
        security = @SecurityRequirement(name = "cookieAuth"))
@SecurityScheme(name = "cookieAuth", type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE, paramName = "token")
public class OpenApiConfig {
}