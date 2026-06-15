package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedById;
    private final Instant createdAt;

    private RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant revokedAt, UUID replacedById, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.revokedAt = revokedAt;
        this.replacedById = replacedById;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, expiresAt, null, null, createdAt);
    }

    public static RefreshToken rehydrate(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt,
            UUID replacedById,
            Instant createdAt
    ) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt, replacedById, createdAt);
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(Instant revokedAt, UUID replacedById) {
        if (this.revokedAt == null) {
            this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
            this.replacedById = replacedById;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public UUID replacedById() {
        return replacedById;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
