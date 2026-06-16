package com.czx.wenshu.infrastructure.storage;

import com.czx.wenshu.application.storage.FileStorageService;
import java.io.InputStream;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件存储降级实现（P9-04）。
 *
 * <p>当腾讯云 COS 凭据未配置时，{@link CosStorageConfig} 注册此 Bean。
 * 所有上传操作抛出 {@link UnsupportedOperationException}，
 * 预签名 URL 返回占位符，不影响应用启动和非存储相关功能。</p>
 */
public class NoopFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(NoopFileStorageService.class);

    /**
     * 上传操作不可用，抛出运行时异常。
     * 调用方（如 DataExportTaskRunner）应在任务失败时捕获并标记任务状态。
     */
    @Override
    public String upload(String objectKey, InputStream inputStream,
                         String contentType, long sizeBytes) {
        log.warn("[NoopFileStorageService] COS 凭据未配置，上传操作不可用 objectKey={}", objectKey);
        throw new UnsupportedOperationException("COS 凭据未配置，无法上传文件。请设置 TENCENT_COS_SECRET_ID 和 TENCENT_COS_SECRET_KEY。");
    }

    /**
     * 返回占位 URL 字符串（含提示说明）。
     */
    @Override
    public String generatePresignedUrl(String objectKey, Duration ttl) {
        log.warn("[NoopFileStorageService] COS 凭据未配置，返回占位 URL objectKey={}", objectKey);
        return "https://cos.not-configured/" + objectKey + "?reason=COS_NOT_CONFIGURED";
    }

    /**
     * 凭据未配置，返回 false。
     */
    @Override
    public boolean isAvailable() {
        return false;
    }
}
