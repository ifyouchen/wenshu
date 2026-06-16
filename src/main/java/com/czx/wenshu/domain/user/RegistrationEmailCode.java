package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class RegistrationEmailCode {

    private final UUID id;
    private final EmailAddress email;
    private final String codeHash;
    private final Instant expiresAt;
    private Instant usedAt;
    private final Instant createdAt;

    private RegistrationEmailCode(UUID id, EmailAddress email, String codeHash,
                                  Instant expiresAt, Instant usedAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.codeHash = Objects.requireNonNull(codeHash, "codeHash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.usedAt = usedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static RegistrationEmailCode issue(String email, String codeHash, Instant expiresAt, Instant createdAt) {
        return new RegistrationEmailCode(UUID.randomUUID(), new EmailAddress(email), codeHash, expiresAt, null, createdAt);
    }

    public static RegistrationEmailCode rehydrate(UUID id, String email, String codeHash,
                                                  Instant expiresAt, Instant usedAt, Instant createdAt) {
        return new RegistrationEmailCode(id, new EmailAddress(email), codeHash, expiresAt, usedAt, createdAt);
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

    public EmailAddress email() {
        return email;
    }

    public String codeHash() {
        return codeHash;
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
