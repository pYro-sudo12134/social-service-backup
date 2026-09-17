package by.losik.apigateway.service;

import by.losik.apigateway.annotation.Loggable;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
@Loggable(logResult = true, level = Loggable.Level.DEBUG)
@RequiredArgsConstructor
public class GatewayJwtService {

    @Value("${spring.jwt.secret}")
    private String SECRET;

    @Value("${spring.jwt.expiration:86400000}")
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiration;

    private final TokenBlacklistService tokenBlacklistService;
    private final UserStatusService userStatusService;

    private Key signingKey;

    @PostConstruct
    public void init() {
        if (SECRET == null || SECRET.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("user_id", userId)
                .claim("token_type", "access")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("user_id", userId)
                .claim("token_type", "refresh")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(signingKey)
                .compact();
    }

    public Mono<String> getTokenType(String token) {
        return extractClaim(token, claims -> claims.get("token_type", String.class));
    }

    public Mono<String> refreshAccessToken(String refreshToken) {
        return validateToken(refreshToken)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new RuntimeException("Invalid refresh token"));
                    }

                    return getTokenType(refreshToken)
                            .flatMap(tokenType -> {
                                if (!"refresh".equals(tokenType)) {
                                    return Mono.error(new RuntimeException("Not a refresh token"));
                                }

                                return extractUserId(refreshToken)
                                        .zipWith(extractUsername(refreshToken))
                                        .flatMap(tuple -> {
                                            Long userId = tuple.getT1();
                                            String username = tuple.getT2();

                                            return userStatusService.validateUserIdentity(userId, username)
                                                    .flatMap(valid -> {
                                                        if (!valid) {
                                                            return Mono.error(new RuntimeException("User identity validation failed"));
                                                        }
                                                        return Mono.just(generateToken(userId, username));
                                                    });
                                        });
                            });
                });
    }

    public Mono<String> extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Mono<Date> extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Mono<Instant> extractExpirationAsInstant(String token) {
        return extractExpiration(token)
                .map(Date::toInstant);
    }

    private <T> Mono<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        return Mono.fromCallable(() -> {
            Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        });
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Mono<Long> extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userIdObj = claims.get("user_id");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            }
            userIdObj = claims.get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            }
            return null;
        });
    }

    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
                    try {
                        return extractAllClaims(token);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .flatMap(claims -> {
                    if (claims == null) {
                        return Mono.just(false);
                    }

                    return tokenBlacklistService.isTokenBlacklisted(token)
                            .flatMap(isBlacklisted -> {
                                if (isBlacklisted) {
                                    return Mono.just(false);
                                }

                                Object userIdObj = claims.get("user_id");
                                String username = claims.getSubject();

                                if (userIdObj instanceof Number && username != null) {
                                    Long userId = ((Number) userIdObj).longValue();

                                    return userStatusService.isUserActive(userId)
                                            .flatMap(isActive -> {
                                                if (!isActive) {
                                                    return Mono.just(false);
                                                }
                                                return userStatusService.isUserEnabled(userId);
                                            })
                                            .flatMap(isEnabled -> {
                                                if (!isEnabled) {
                                                    return Mono.just(false);
                                                }
                                                return userStatusService.validateUserIdentity(userId, username);
                                            });
                                }

                                return Mono.just(false);
                            });
                })
                .onErrorReturn(false);
    }

    public Mono<Boolean> revokeToken(String token) {
        return extractExpirationAsInstant(token)
                .flatMap(expiresAt ->
                        tokenBlacklistService.addToBlacklist(token, expiresAt)
                )
                .onErrorReturn(false);
    }
}