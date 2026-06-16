package com.czx.wenshu.domain.user;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final EmailAddress email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private IdentityType identityType;
    private boolean emailVerified;
    private boolean aiTrainConsent;
    private int loginFailCount;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private boolean deleted;
    private Instant deletedAt;
    private int dailyCharGoal;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(
            UUID id,
            EmailAddress email,
            String passwordHash,
            String nickname,
            String avatarUrl,
            IdentityType identityType,
            boolean emailVerified,
            boolean aiTrainConsent,
            int loginFailCount,
            Instant lockedUntil,
            Instant lastLoginAt,
            boolean deleted,
            Instant deletedAt,
            int dailyCharGoal,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        setPasswordHash(passwordHash);
        this.nickname = normalizeNickname(nickname);
        this.avatarUrl = avatarUrl;
        this.identityType = identityType == null ? IdentityType.NEW_AUTHOR : identityType;
        this.emailVerified = emailVerified;
        this.aiTrainConsent = aiTrainConsent;
        this.loginFailCount = loginFailCount;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.dailyCharGoal = Math.max(0, dailyCharGoal);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (deleted && deletedAt == null) {
            throw new IllegalArgumentException("删除状态必须记录删除时间");
        }
    }

    public static User register(String email, String passwordHash, String nickname, Clock clock) {
        Instant now = Instant.now(clock);
        return new User(
                UUID.randomUUID(),
                new EmailAddress(email),
                passwordHash,
                nickname,
                null,
                IdentityType.NEW_AUTHOR,
                false,
                true,
                0,
                null,
                null,
                false,
                null,
                2000,
                now,
                now
        );
    }

    public static User rehydrate(
            UUID id,
            String email,
            String passwordHash,
            String nickname,
            String avatarUrl,
            IdentityType identityType,
            boolean emailVerified,
            boolean aiTrainConsent,
            int loginFailCount,
            Instant lockedUntil,
            Instant lastLoginAt,
            boolean deleted,
            Instant deletedAt,
            int dailyCharGoal,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new User(id, new EmailAddress(email), passwordHash, nickname, avatarUrl, identityType, emailVerified,
                aiTrainConsent, loginFailCount, lockedUntil, lastLoginAt, deleted, deletedAt,
                dailyCharGoal, createdAt, updatedAt);
    }

    public void updateDailyCharGoal(int newGoal, Clock clock) {
        this.dailyCharGoal = Math.max(0, newGoal);
        this.updatedAt = Instant.now(clock);
    }

    public void markDeleted(Clock clock) {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = Instant.now(clock);
        this.updatedAt = this.deletedAt;
    }

    public void restore(Clock clock) {
        if (!deleted) {
            return;
        }
        this.deleted = false;
        this.deletedAt = null;
        this.updatedAt = Instant.now(clock);
    }

    public void verifyEmail(Clock clock) {
        if (emailVerified) {
            return;
        }
        this.emailVerified = true;
        this.updatedAt = Instant.now(clock);
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void recordLoginFailure(Clock clock) {
        Instant now = Instant.now(clock);
        this.loginFailCount += 1;
        if (this.loginFailCount >= 5) {
            this.lockedUntil = now.plusSeconds(15 * 60L);
        }
        this.updatedAt = now;
    }

    public void recordLoginSuccess(Clock clock) {
        Instant now = Instant.now(clock);
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = now;
        this.updatedAt = now;
    }

    public void changePassword(String passwordHash, Clock clock) {
        setPasswordHash(passwordHash);
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now(clock);
    }

    public void updateProfile(String nickname, String avatarUrl, IdentityType identityType, Clock clock) {
        this.nickname = normalizeNickname(nickname);
        this.avatarUrl = avatarUrl;
        if (identityType != null) {
            this.identityType = identityType;
        }
        this.updatedAt = Instant.now(clock);
    }

    public void updateAiConsent(boolean aiTrainConsent, Clock clock) {
        this.aiTrainConsent = aiTrainConsent;
        this.updatedAt = Instant.now(clock);
    }

    public void changePasswordByUser(String currentPasswordHash, String newPasswordHash, Clock clock) {
        setPasswordHash(newPasswordHash);
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String nickname() {
        return nickname;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public IdentityType identityType() {
        return identityType;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isAiTrainConsent() {
        return aiTrainConsent;
    }

    public int loginFailCount() {
        return loginFailCount;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public int dailyCharGoal() {
        return dailyCharGoal;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("密码哈希不能为空");
        }
        this.passwordHash = passwordHash;
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }
        String trimmed = nickname.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("昵称不能超过 100 个字符");
        }
        return trimmed;
    }
}