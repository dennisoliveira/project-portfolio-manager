package com.github.dennisoliveira.portfolio.integration.members;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
public class MemberClient {

    private final WebClient client;

    public MemberClient(WebClient membersWebClient) {
        this.client = membersWebClient;
    }

    public Optional<ExternalMemberDTO> getById(String id) {
        return client.get()
                .uri("/members/{id}", id)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, resp -> Mono.empty())
                .bodyToMono(ExternalMemberDTO.class)
                .map(Optional::of)
                .switchIfEmpty(Mono.just(Optional.empty()))
                .block();
    }

    public ExternalMemberDTO create(String name, String role) {
        return client.post()
                .uri("/members")
                .bodyValue(Map.of("name", name, "role", role))
                .retrieve()
                .bodyToMono(ExternalMemberDTO.class)
                .block();
    }
}