package by.losik.apigateway.controller;

import by.losik.apigateway.dto.CreateUserDTO;
import by.losik.apigateway.dto.LoginRequest;
import by.losik.apigateway.service.GatewayJwtService;
import by.losik.apigateway.service.UserStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "API для аутентификации и управления токенами")
@RequiredArgsConstructor
public class AuthController {

    private final GatewayJwtService jwtService;
    private final UserStatusService userStatusService;

    @Operation(
            summary = "Приветственное сообщение",
            description = "Публичный endpoint, не требующий аутентификации"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное приветствие",
            content = @Content(mediaType = "text/plain")
    )
    @GetMapping("/welcome")
    public Mono<String> welcome() {
        return Mono.just("Welcome to API Gateway - this endpoint is not secure");
    }

    @Operation(
            summary = "Проверка валидности токена",
            description = "Проверяет валидность JWT токена"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Результат проверки токена",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"valid\": true}"))
            )
    })
    @GetMapping("/validate-token")
    public Mono<ResponseEntity<Map<String, Boolean>>> validateToken(
            @Parameter(
                    description = "JWT токен в формате Bearer token",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    in = ParameterIn.HEADER
            )
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.ok(Map.of("valid", false)));
        }

        String token = authHeader.substring(7);
        return jwtService.validateToken(token)
                .map(valid -> ResponseEntity.ok(Map.of("valid", valid)))
                .defaultIfEmpty(ResponseEntity.ok(Map.of("valid", false)));
    }

    @Operation(
            summary = "Проверка здоровья сервиса",
            description = "Возвращает статус работы API Gateway"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Сервис работает нормально",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"status\": \"API Gateway is running\", \"service\": \"api-gateway\", \"timestamp\": \"2024-01-01T12:00:00Z\"}"))
    )
    @GetMapping("/health")
    public Mono<Map<String, String>> healthCheck() {
        return Mono.just(Map.of(
                "status", "API Gateway is running",
                "service", "api-gateway",
                "timestamp", Instant.now().toString()
        ));
    }

    @Operation(
            summary = "Выход из системы",
            description = "Выполняет logout пользователя, отзывая токен и очищая cookie"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный выход из системы",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"message\": \"Logout successful\"}"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(
            @Parameter(
                    description = "JWT токен для отзыва",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    in = ParameterIn.HEADER
            )
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @NonNull ServerHttpResponse response) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.revokeToken(token);
        }

        ResponseCookie cookie = ResponseCookie.from("JWT", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addCookie(cookie);

        return Mono.just(ResponseEntity.ok()
                .body(Map.of("message", "Logout successful")));
    }

    @Operation(
            summary = "Получение информации о пользователе",
            description = "Возвращает информацию о пользователе из JWT токена"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение информации о пользователе",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"username\": \"john_doe\", \"userId\": 123, \"authenticated\": true}"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Невалидный или отсутствующий токен",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"error\": \"Missing or invalid authorization header\"}"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/user-info")
    public Mono<ResponseEntity<Map<String, Object>>> getUserInfo(
            @Parameter(
                    description = "JWT токен в формате Bearer token",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    in = ParameterIn.HEADER
            )
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> errorBody = Map.of("error", "Missing or invalid authorization header");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
        }

        String token = authHeader.substring(7);

        return jwtService.validateToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        Map<String, Object> errorBody = Map.of("error", "Invalid token");
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
                    }

                    return jwtService.extractUsername(token)
                            .zipWith(jwtService.extractUserId(token))
                            .map(tuple -> {
                                String username = tuple.getT1();
                                Long userId = tuple.getT2();

                                Map<String, Object> successBody = Map.of(
                                        "username", username,
                                        "userId", userId,
                                        "authenticated", true
                                );
                                return ResponseEntity.ok(successBody);
                            });
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Token validation failed: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
                });
    }

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя в системе"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"message\": \"User registered successfully\", \"userId\": 123}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные данные запроса или пользователь уже существует"
            )
    })
    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, Object>>> register(
            @Valid @RequestBody CreateUserDTO registerRequest) {

        return userStatusService.registerUser(registerRequest)
                .map(userId -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "message", "User registered successfully",
                                "userId", userId
                        )));
    }

    @Operation(
            summary = "Аутентификация пользователя",
            description = "Выполняет вход пользователя в систему и возвращает JWT токены"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"accessToken\": \"jwt_token_here\", \"refreshToken\": \"refresh_token_here\", \"username\": \"john_doe\"}"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверные учетные данные"
            )
    })
    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            ServerHttpResponse response) {

        return userStatusService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword())
                .flatMap(userInfo -> {
                    String accessToken = jwtService.generateToken(userInfo.getUserId(), userInfo.getUsername());
                    String refreshToken = jwtService.generateRefreshToken(userInfo.getUserId(), userInfo.getUsername());

                    ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(Duration.ofHours(24))
                            .build();

                    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                            .httpOnly(true)
                            .secure(false)
                            .path("/auth/refresh")
                            .maxAge(Duration.ofDays(7))
                            .build();

                    response.addCookie(accessCookie);
                    response.addCookie(refreshCookie);

                    Map<String, Object> responseBody = Map.of(
                            "accessToken", accessToken,
                            "refreshToken", refreshToken,
                            "username", userInfo.getUsername(),
                            "userId", userInfo.getUserId(),
                            "message", "Login successful"
                    );

                    return Mono.just(ResponseEntity.ok().body(responseBody));
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Authentication failed: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
                });
    }

    @Operation(
            summary = "Обновление access token",
            description = "Обновляет access token с помощью refresh token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Токен успешно обновлен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"accessToken\": \"new_jwt_token_here\"}"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Невалидный refresh token"
            )
    })
    @PostMapping("/refresh")
    public Mono<ResponseEntity<Map<String, Object>>> refreshToken(
            @Parameter(
                    description = "Refresh token в cookie или теле запроса",
                    required = false
            )
            @CookieValue(value = "refresh_token", required = false) String refreshTokenCookie,
            @RequestBody(required = false) Map<String, String> body,
            ServerHttpResponse response) {

        String refreshToken = refreshTokenCookie;
        if (refreshToken == null && body != null) {
            refreshToken = body.get("refreshToken");
        }

        if (refreshToken == null) {
            Map<String, Object> errorBody = Map.of("error", "Refresh token is required");
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody));
        }

        return jwtService.refreshAccessToken(refreshToken)
                .flatMap(newAccessToken -> {
                    ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken)
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(Duration.ofHours(24))
                            .build();

                    response.addCookie(accessCookie);

                    Map<String, Object> responseBody = Map.of(
                            "accessToken", newAccessToken,
                            "message", "Token refreshed successfully"
                    );

                    return Mono.just(ResponseEntity.ok().body(responseBody));
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Token refresh failed: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
                });
    }

    @Operation(
            summary = "Выход из системы (полный)",
            description = "Выполняет logout пользователя, отзывая оба токена"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный выход из системы",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Logout successful\"}"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-full")
    public Mono<ResponseEntity<Map<String, String>>> logoutFull(
            @Parameter(description = "JWT токен для отзыва", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            @NonNull ServerHttpResponse response) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.revokeToken(token);
        }

        if (refreshToken != null) {
            jwtService.revokeToken(refreshToken);
        }

        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh")
                .maxAge(Duration.ZERO)
                .build();

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return Mono.just(ResponseEntity.ok()
                .body(Map.of("message", "Logout successful")));
    }
}