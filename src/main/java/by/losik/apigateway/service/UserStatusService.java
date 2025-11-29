package by.losik.apigateway.service;

import by.losik.apigateway.annotation.Loggable;
import by.losik.apigateway.dto.CreateUserDTO;
import by.losik.apigateway.dto.UserInfo;
import by.losik.apigateway.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
public class UserStatusService {

    private final WebClient.Builder webClientBuilder;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    public Mono<Boolean> isUserActive(Long userId) {
        return webClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/api/users/exists/{id}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new RuntimeException("User service unavailable")))
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean exists = (Boolean) response.get("exists");
                    return exists != null && exists;
                })
                .onErrorReturn(false);
    }

    public Mono<Boolean> isUserEnabled(Long userId) {
        return webClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/api/users/{id}/status", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new RuntimeException("Failed to get user status")))
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean enabled = (Boolean) response.get("enabled");
                    return enabled == null || enabled;
                })
                .onErrorReturn(true);
    }

    public Mono<Long> registerUser(CreateUserDTO registerRequest) {
        return webClientBuilder.build()
                .post()
                .uri(userServiceUrl + "/api/users")
                .bodyValue(registerRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody ->
                                        Mono.error(new RuntimeException("Registration failed: " + errorBody))
                                )
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RuntimeException("User service unavailable"))
                )
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Object idObj = response.get("id");
                    if (idObj instanceof Number) {
                        return Mono.just(((Number) idObj).longValue());
                    } else {
                        return Mono.error(new RuntimeException("Invalid response from user service"));
                    }
                })
                .onErrorMap(throwable -> {
                    if (throwable.getMessage().contains("already exists")) {
                        return new RuntimeException("User with this username or email already exists");
                    }
                    return new RuntimeException("Registration failed: " + throwable.getMessage());
                });
    }

    public Mono<UserInfo> authenticateUser(String username, String password) {
        return webClientBuilder.build()
                .post()
                .uri(userServiceUrl + "/api/users/verify-credentials")
                .bodyValue(Map.of(
                        "username", username,
                        "password", password
                ))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new RuntimeException("Invalid credentials"))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RuntimeException("User service unavailable"))
                )
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Object idObj = response.get("id");
                    Object usernameObj = response.get("username");

                    if (idObj instanceof Number && usernameObj instanceof String) {
                        UserInfo userInfo = new UserInfo();
                        userInfo.setUserId(((Number) idObj).longValue());
                        userInfo.setUsername((String) usernameObj);
                        return Mono.just(userInfo);
                    } else {
                        return Mono.error(new RuntimeException("Invalid response from user service"));
                    }
                });
    }

    public Mono<Boolean> validateUserIdentity(Long userId, String username) {
        return webClientBuilder.build()
                .post()
                .uri(userServiceUrl + "/api/users/validate-identity")
                .bodyValue(Map.of(
                        "userId", userId,
                        "username", username
                ))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.just(new AuthenticationException("4xx")))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.just(new AuthenticationException("5xx")))
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean valid = (Boolean) response.get("valid");
                    return valid != null && valid;
                })
                .onErrorReturn(false);
    }
}