package com.johnvo.retailhub.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ObjectMapper infrastructureObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
