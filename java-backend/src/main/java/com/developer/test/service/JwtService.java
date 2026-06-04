package com.developer.test.service;

import com.developer.test.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long ttlSeconds;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.security.jwt.secret:dev-jwt-secret}") String secret,
                      @Value("${app.security.jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(User user) {
        try {
            long issuedAt = Instant.now().getEpochSecond();
            long expiresAt = issuedAt + ttlSeconds;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getEmail());
            payload.put("uid", user.getId());
            payload.put("name", user.getName());
            payload.put("role", user.getRole());
            payload.put("iat", issuedAt);
            payload.put("exp", expiresAt);

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson = objectMapper.writeValueAsString(payload);
            String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
            String body = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = sign(header + "." + body);
            return header + "." + body + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create JWT", ex);
        }
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public JwtPrincipal parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!expectedSignature.equals(parts[2])) {
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, Map.class);
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return null;
            }

            JwtPrincipal principal = new JwtPrincipal();
            principal.setEmail(String.valueOf(payload.get("sub")));
            principal.setUserId(((Number) payload.get("uid")).intValue());
            principal.setName(String.valueOf(payload.get("name")));
            principal.setRole(String.valueOf(payload.get("role")));
            principal.setExpiresAt(expiresAt);
            return principal;
        } catch (Exception ex) {
            return null;
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class JwtPrincipal {
        private int userId;
        private String email;
        private String name;
        private String role;
        private long expiresAt;

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
