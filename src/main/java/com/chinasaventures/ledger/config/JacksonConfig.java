package com.chinasaventures.ledger.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // CHANGED: new bean — tells Jackson to resolve lazy Hibernate proxies
    // properly instead of crashing when it hits one (e.g. recordedBy.branch)
    @Bean
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }
}