package com.czx.wenshu.infrastructure.storage;

import com.czx.wenshu.application.storage.FileStorageService;
import com.czx.wenshu.infrastructure.config.WenshuProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 存储配置（P9-04）。
 *
 * <p>根据 {@code wenshu.storage.secret-id} 和 {@code wenshu.storage.secret-key} 是否配置，
 * 分别注册 {@link CosStorageService}（真实 COS）或 {@link NoopFileStorageService}（降级）。</p>
 *
 * <p>降级策略：凭据未配置时应用正常启动，上传操作抛出可捕获的异常，
 * 不影响非存储相关功能。</p>
 */
@Configuration
public class CosStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(CosStorageConfig.class);

    /**
     * 注册文件存储服务 Bean。
     *
     * @param props 应用配置（包含 COS 凭据）
     * @return COS 实现（凭据已配置）或 Noop 降级实现（凭据未配置）
     */
    @Bean
    public FileStorageService fileStorageService(WenshuProperties props) {
        WenshuProperties.Storage storage = props.getStorage();
        String secretId = storage.getSecretId();
        String secretKey = storage.getSecretKey();

        if (secretId != null && !secretId.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            log.info("[CosStorageConfig] 腾讯云 COS 凭据已配置，启用真实存储。bucket={} region={}",
                    storage.getBucket(), storage.getRegion());
            COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
            ClientConfig clientConfig = new ClientConfig(new Region(storage.getRegion()));
            COSClient cosClient = new COSClient(credentials, clientConfig);
            return new CosStorageService(cosClient, storage);
        }

        log.warn("[CosStorageConfig] 腾讯云 COS 凭据未配置，存储功能降级为 Noop，上传操作将不可用。");
        return new NoopFileStorageService();
    }
}
