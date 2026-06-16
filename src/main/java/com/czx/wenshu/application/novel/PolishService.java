package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** 基础校正、进阶润色、风格重塑（P5-09）。 */
@Service
public class PolishService {

    private static final Logger log = LoggerFactory.getLogger(PolishService.class);

    private static final TypeReference<List<PolishAnnotation>> ANNOTATION_LIST_TYPE = new TypeReference<>() {};

    private final LlmClient creativeLlmClient;
    private final LlmClient utilityLlmClient;
    private final ObjectMapper objectMapper;

    public PolishService(@Qualifier("creativeLlmClient") LlmClient creativeLlmClient,
                          @Qualifier("utilityLlmClient") LlmClient utilityLlmClient,
                          ObjectMapper objectMapper) {
        this.creativeLlmClient = creativeLlmClient;
        this.utilityLlmClient = utilityLlmClient;
        this.objectMapper = objectMapper;
    }

    /** 基础校正：返回逐条修改建议，使用工具模型（结构化 JSON 输出）。 */
    public PolishResult basicCorrection(String text) {
        if (text == null || text.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "待校正文本不能为空");
        }
        long start = System.currentTimeMillis();
        PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/polish_basic.txt");
        String prompt = tpl.fill(Map.of("text", text));
        String response = utilityLlmClient.chat(null, prompt);
        List<PolishAnnotation> annotations = JsonExtractor.parseArray(response, ANNOTATION_LIST_TYPE, objectMapper);
        long elapsed = System.currentTimeMillis() - start;
        log.info("[PolishService] 基础校正完成 文本长度={} 耗时={}ms 建议数={}", text.length(), elapsed, annotations != null ? annotations.size() : 0);
        return PolishResult.ofAnnotations(annotations != null ? annotations : List.of());
    }

    /** 进阶润色：返回完整改写文本，使用创意模型。 */
    public PolishResult advancedPolish(String text, String instruction) {
        if (text == null || text.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "待润色文本不能为空");
        }
        long start = System.currentTimeMillis();
        PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/polish_advanced.txt");
        String prompt = tpl.fill(Map.of(
                "text", text,
                "instruction", instruction != null ? instruction : ""));
        String rewritten = creativeLlmClient.chat(null, prompt);
        long elapsed = System.currentTimeMillis() - start;
        log.info("[PolishService] 进阶润色完成 文本长度={} 耗时={}ms 输出长度={}", text.length(), elapsed, rewritten != null ? rewritten.length() : 0);
        return PolishResult.ofRewritten("advanced", rewritten);
    }

    /** 风格重塑：按指定风格改写文本，使用创意模型。 */
    public PolishResult styleRewrite(String text, String styleDescription) {
        if (text == null || text.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "待改写文本不能为空");
        }
        if (styleDescription == null || styleDescription.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "风格描述不能为空");
        }
        long start = System.currentTimeMillis();
        PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/polish_style.txt");
        String prompt = tpl.fill(Map.of("text", text, "styleDescription", styleDescription));
        String rewritten = creativeLlmClient.chat(null, prompt);
        long elapsed = System.currentTimeMillis() - start;
        log.info("[PolishService] 风格重塑完成 文本长度={} 耗时={}ms 输出长度={}", text.length(), elapsed, rewritten != null ? rewritten.length() : 0);
        return PolishResult.ofRewritten("style", rewritten);
    }
}
