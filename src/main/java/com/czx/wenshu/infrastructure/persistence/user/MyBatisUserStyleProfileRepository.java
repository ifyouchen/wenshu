package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.UserStyleProfile;
import com.czx.wenshu.domain.user.UserStyleProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisUserStyleProfileRepository implements UserStyleProfileRepository {

    private final UserStyleProfileMapper mapper;

    public MyBatisUserStyleProfileRepository(UserStyleProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(UserStyleProfile profile) {
        UserStyleProfileRecord record = toRecord(profile);
        if (mapper.findByUserId(profile.userId().toString()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
    }

    @Override
    public Optional<UserStyleProfile> findByUserId(UUID userId) {
        return Optional.ofNullable(mapper.findByUserId(userId.toString())).map(this::toDomain);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        mapper.deleteByUserId(userId.toString());
    }

    private UserStyleProfile toDomain(UserStyleProfileRecord r) {
        return UserStyleProfile.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getUserId()),
                r.getSampleText(),
                r.getStyleTags(),
                r.getAnalysisTaskId() != null ? UUID.fromString(r.getAnalysisTaskId()) : null,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private UserStyleProfileRecord toRecord(UserStyleProfile p) {
        UserStyleProfileRecord r = new UserStyleProfileRecord();
        r.setId(p.id().toString());
        r.setUserId(p.userId().toString());
        r.setSampleText(p.sampleText());
        r.setStyleTags(p.styleTags());
        r.setAnalysisTaskId(p.analysisTaskId() != null ? p.analysisTaskId().toString() : null);
        r.setCreatedAt(p.createdAt());
        r.setUpdatedAt(p.updatedAt());
        return r;
    }
}
