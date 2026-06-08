package com.orderflux.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenApiConfig — Swagger/OpenAPI documentation configuration.
 *
 * What this configures:
 *   1. API metadata (title, version, description, contact)
 *   2. JWT security scheme — adds "Authorize" button to Swagger UI
 *      so you can enter your token once and test all protected endpoints
 *
 * After startup, visit:
 *   http://localhost:8080/api/swagger-ui/index.html  → interactive UI
 *   http://localhost:8080/api/v3/api-docs            → raw JSON spec
 */
@Configuration
public class OpenApiConfig {
	/**
     * The security scheme name must match exactly what you reference
     * in @SecurityRequirement annotations on controllers.
     */
	private static final String SECURITY_SCHEME_NAME="bearerAuth";
	
	@Bean
	public OpenAPI orderFluxOpenAPI() {
		return new OpenAPI()
				// API metadata
				.info(new Info()
						.title("OrderFlux E-Commerce API")
						.description(
								"""
                                Production-grade E-Commerce Backend REST API.
                                
                                Authentication:
                                  1. Register via POST /auth/register
                                  2. Login via POST /auth/login → copy token
                                  3. Click 'Authorize' button → paste token
                                  4. All protected endpoints now work
                                """
								)
						.version("1.0.0")
						.contact(new Contact()
								.name("Sudharsanselvan T")
								.email("sudharsanselvan13@gmail.com")
								.url("https://sudharsanselvan.netlify.app"))
								.license(new License()
										.name("MIT Licence")
										.url("https://opensource.org/licenses/MIT"))
								)
				// Global security requirement — applies JWT to all endpoints
				.addSecurityItem(
						new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
						)
				//Define the JWT Bearer security scheme
				.components(new Components()
						.addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
								.name(SECURITY_SCHEME_NAME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Enter your JWT token. "+"Get it from POST /auth/login")
								)
						);
	}
}
