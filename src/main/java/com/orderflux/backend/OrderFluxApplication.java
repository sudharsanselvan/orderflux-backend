package com.orderflux.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @SpringBootApplication is a meta-annotation combining:
 *
 * 1. @Configuration
 *    → This class can define @Bean methods
 *
 * 2. @EnableAutoConfiguration
 *    → Spring Boot reads your classpath and auto-configures:
 *       - Saw MySQL driver? → Configures DataSource
 *       - Saw Spring Security? → Configures security filter chain
 *       - Saw Spring Web? → Starts embedded Tomcat
 *
 * 3. @ComponentScan
 *    → Scans this package AND all sub-packages for:
 *       @Component, @Service, @Repository, @Controller
 *    → This is why package structure matters — everything must
 *       be inside com.ecommerce.backend
 */
@SpringBootApplication
public class OrderFluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderFluxApplication.class, args);
    }
}