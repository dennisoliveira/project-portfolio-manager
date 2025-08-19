package com.github.dennisoliveira.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MemberClientConfig {

    @Bean
    public WebClient membersWebClient(@Value("${members.api.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}