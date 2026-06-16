package com.czx.wenshu.interfaces.rest.novel;

import com.czx.wenshu.application.novel.PolishResult;
import com.czx.wenshu.application.novel.PolishService;
import com.czx.wenshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Polish", description = "文本润色：基础校正、进阶润色、风格重塑（P5-09）")
@Validated
@RestController
@RequestMapping("/api/v1/polish")
public class PolishController {

    private final PolishService polishService;

    public PolishController(PolishService polishService) {
        this.polishService = polishService;
    }

    @Operation(summary = "基础校正",
               description = "检查错别字、语法、标点问题，返回逐条修改建议。")
    @PostMapping("/basic")
    public Result<PolishResult> basicCorrection(@Valid @RequestBody PolishRequest request) {
        return Result.ok(polishService.basicCorrection(request.text()));
    }

    @Operation(summary = "进阶润色",
               description = "优化句式和描写，保持情节不变，返回改写后的完整文本。")
    @PostMapping("/advanced")
    public Result<PolishResult> advancedPolish(@Valid @RequestBody PolishRequest request) {
        return Result.ok(polishService.advancedPolish(request.text(), request.instruction()));
    }

    @Operation(summary = "风格重塑",
               description = "按指定风格改写文本，保持核心情节和人物关系不变。")
    @PostMapping("/style")
    public Result<PolishResult> styleRewrite(@Valid @RequestBody PolishRequest request) {
        return Result.ok(polishService.styleRewrite(request.text(), request.styleDescription()));
    }
}
