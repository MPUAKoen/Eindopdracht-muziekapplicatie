package com.example.demo.security;

import com.example.demo.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secretKey;
    private final long expirationMs;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.objectMapper = objectMapper;
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", user.getEmail());
        payload.put("role", user.getRole());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusMillis(expirationMs).getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;

        return signingInput + "." + URL_ENCODER.encodeToString(sign(signingInput));
    }

    public Optional<String> extractEmail(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return Optional.empty();
            }

            JsonNode payload = objectMapper.readTree(URL_DECODER.decode(parts[1]));
            long expiresAt = payload.path("exp").asLong(0L);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            String subject = payload.path("sub").asText(null);
            return Optional.ofNullable(subject);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize JWT payload", ex);
        }
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign JWT", ex);
        }
    }
}
