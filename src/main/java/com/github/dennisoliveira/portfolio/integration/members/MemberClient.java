package com.github.dennisoliveira.portfolio.integration.members;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(ExternalMemberDTO.class);
                    }
                    if (resp.statusCode().value() == 404) {
                        return Mono.empty();
                    }

                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new ExternalServiceException(
                                    "Members API error: " + resp.statusCode() + " " + body)));
                })
                .timeout(Duration.ofSeconds(5))
                .map(Optional::of)
                .switchIfEmpty(Mono.just(Optional.empty()))
                .onErrorResume(WebClientResponseException.class, e ->

                        Mono.just(Optional.empty()))
                .block();
    }

    public ExternalMemberDTO create(String name, String role) {
        return client.post()
                .uri("/members")
                .bodyValue(Map.of("name", name, "role", role))
                .retrieve()
                .bodyToMono(ExternalMemberDTO.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }
}
