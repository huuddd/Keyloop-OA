package com.keyloop.scheduler.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
    // Enables @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy
}
