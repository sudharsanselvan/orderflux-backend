package com.orderflux.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JpaConfig — JPA/Hibernate configuration.
 *
 * @EnableJpaAuditing:
 *   Activates Spring Data's auditing mechanism.
 *   Without this, @CreatedDate and @LastModifiedDate do NOTHING.
 *   This is a very common beginner mistake — annotations with
 *   no effect because the feature isn't enabled.
 *
 * @EnableJpaRepositories:
 *   Tells Spring where to scan for Repository interfaces.
 *   Usually auto-detected, but explicit is better.
 */

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages="com.orderflux.backend.repository")
public class JpaConfig {

}
