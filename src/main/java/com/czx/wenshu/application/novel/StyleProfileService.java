package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.task.AsyncTask;
import com.czx.wenshu.domain.user.UserStyleProfile;
import com.czx.wenshu.domain.user.UserStyleProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 文风档案管理服务（P5-10）。 */
@Service
public class StyleProfileService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final UserStyleProfileRepository profileRepository;
    private final AsyncTaskService asyncTaskService;
    private final StyleAnalysisTaskRunner styleAnalysisTaskRunner;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StyleProfileService(UserStyleProfileRepository profileRepository,
                                AsyncTaskService asyncTaskService,
                                StyleAnalysisTaskRunner styleAnalysisTaskRunner,
                                ObjectMapper objectMapper,
                                Clock clock) {
        this.profileRepository = profileRepository;
        this.asyncTaskService = asyncTaskService;
        this.styleAnalysisTaskRunner = styleAnalysisTaskRunner;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserStyleProfileInfo getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> UserStyleProfileInfo.from(p, parseTags(p.styleTags())))
                .orElse(new UserStyleProfileInfo(userId.toString(), List.of(), null, false, null));
    }

    @Transactional
    public UserStyleProfileInfo saveProfile(UUID userId, String sampleText) {
        if (sampleText == null || sampleText.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "文风样本文本不能为空");
        }
        AsyncTask task = asyncTaskService.createTask(userId, null, "style_analysis");
        UserStyleProfile profile = profileRepository.findByUserId(userId)
                .orElse(UserStyleProfile.create(userId, sampleText, clock));
        profile.updateSample(sampleText, task.id(), clock);
        profileRepository.save(profile);
        styleAnalysisTaskRunner.run(task.id(), userId, sampleText);
        return UserStyleProfileInfo.from(profile, parseTags(profile.styleTags()));
    }

    @Transactional
    public void deleteProfile(UUID userId) {
        profileRepository.deleteByUserId(userId);
    }

    private List<String> parseTags(String json) {
        if (json == null || "[]".equals(json.strip())) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }
}
