package com.czx.wenshu.application.user;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.StyleTemplate;
import com.czx.wenshu.domain.user.StyleTemplateRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StyleTemplateService {

    private final StyleTemplateRepository repository;
    private final Clock clock;

    public StyleTemplateService(StyleTemplateRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StyleTemplateInfo> list(UUID userId, String templateType) {
        List<StyleTemplate> templates = templateType == null || templateType.isBlank()
                ? repository.findByUserId(userId)
                : repository.findByUserIdAndType(userId, normalizeType(templateType));
        return templates.stream().map(StyleTemplateInfo::from).toList();
    }

    @Transactional
    public StyleTemplateInfo create(UUID userId, CreateStyleTemplateCommand command) {
        StyleTemplate template = StyleTemplate.create(userId, requireText(command.name(), "模板名称不能为空"),
                normalizeType(command.templateType()), toJsonArray(command.genres()),
                requireText(command.prompt(), "提示词不能为空"), clock);
        repository.save(template);
        return StyleTemplateInfo.from(template);
    }

    @Transactional
    public StyleTemplateInfo update(UUID templateId, UUID userId, UpdateStyleTemplateCommand command) {
        StyleTemplate template = loadOwned(templateId, userId);
        template.update(command.name(), command.templateType() != null ? normalizeType(command.templateType()) : null,
                command.genres() != null ? toJsonArray(command.genres()) : null, command.prompt(), clock);
        repository.save(template);
        return StyleTemplateInfo.from(template);
    }

    @Transactional
    public StyleTemplateInfo activate(UUID templateId, UUID userId) {
        StyleTemplate template = loadOwned(templateId, userId);
        repository.deactivateByUserIdAndType(userId, template.templateType());
        template.activate(clock);
        repository.save(template);
        return StyleTemplateInfo.from(template);
    }

    @Transactional
    public void delete(UUID templateId, UUID userId) {
        StyleTemplate template = loadOwned(templateId, userId);
        repository.deleteById(template.id());
    }

    private StyleTemplate loadOwned(UUID templateId, UUID userId) {
        StyleTemplate template = repository.findById(templateId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "风格模板不存在"));
        if (!template.userId().equals(userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "风格模板不存在");
        }
        return template;
    }

    private static String normalizeType(String value) {
        String type = requireText(value, "模板类型不能为空").trim();
        if (!"writing".equals(type) && !"polish".equals(type)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "模板类型仅支持 writing 或 polish");
        }
        return type;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }

    static String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .map(v -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
