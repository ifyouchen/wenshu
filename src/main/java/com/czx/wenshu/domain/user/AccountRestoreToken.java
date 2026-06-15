package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AccountRestoreToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant usedAt;
    private final Instant createdAt;

    private AccountRestoreToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.usedAt = usedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AccountRestoreToken issue(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        return new AccountRestoreToken(UUID.randomUUID(), userId, tokenHash, expiresAt, null, createdAt);
    }

    public static AccountRestoreToken rehydrate(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt, Instant createdAt) {
        return new AccountRestoreToken(id, userId, tokenHash, expiresAt, usedAt, createdAt);
    }

    public boolean isUsableAt(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant usedAt) {
        if (this.usedAt == null) {
            this.usedAt = Objects.requireNonNull(usedAt, "usedAt must not be null");
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

    public Instant usedAt() {
        return usedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}