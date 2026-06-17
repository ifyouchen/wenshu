package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.StyleTemplate;
import com.czx.wenshu.domain.user.StyleTemplateRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisStyleTemplateRepository implements StyleTemplateRepository {

    private final StyleTemplateMapper mapper;

    public MyBatisStyleTemplateRepository(StyleTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public StyleTemplate save(StyleTemplate template) {
        StyleTemplateRecord record = toRecord(template);
        if (mapper.findById(template.id().toString()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
        return template;
    }

    @Override
    public Optional<StyleTemplate> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<StyleTemplate> findByUserId(UUID userId) {
        return mapper.findByUserId(userId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<StyleTemplate> findByUserIdAndType(UUID userId, String templateType) {
        return mapper.findByUserIdAndType(userId.toString(), templateType).stream().map(this::toDomain).toList();
    }

    @Override
    public void deactivateByUserIdAndType(UUID userId, String templateType) {
        mapper.deactivateByUserIdAndType(userId.toString(), templateType);
    }

    @Override
    public void deleteById(UUID id) {
        mapper.deleteById(id.toString());
    }

    private StyleTemplate toDomain(StyleTemplateRecord r) {
        return StyleTemplate.rehydrate(UUID.fromString(r.getId()), UUID.fromString(r.getUserId()),
                r.getName(), r.getTemplateType(), r.getGenres(), r.getPrompt(), r.isActive(),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private StyleTemplateRecord toRecord(StyleTemplate template) {
        StyleTemplateRecord r = new StyleTemplateRecord();
        r.setId(template.id().toString());
        r.setUserId(template.userId().toString());
        r.setName(template.name());
        r.setTemplateType(template.templateType());
        r.setGenres(template.genres());
        r.setPrompt(template.prompt());
        r.setActive(template.active());
        r.setCreatedAt(template.createdAt());
        r.setUpdatedAt(template.updatedAt());
        return r;
    }
}
