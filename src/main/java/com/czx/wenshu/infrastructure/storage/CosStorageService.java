package com.czx.wenshu.infrastructure.storage;

import com.czx.wenshu.application.storage.FileStorageService;
import com.czx.wenshu.infrastructure.config.WenshuProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 腾讯云 COS 文件存储服务（P9-04）。
 *
 * <p>实现 {@link FileStorageService} 接口，使用腾讯云 COS SDK 完成：</p>
 * <ul>
 *   <li>文件上传（PUT Object）</li>
 *   <li>生成预签名下载 URL（Presigned URL）</li>
 * </ul>
 *
 * <p>对象 URL 格式：自定义域名优先，否则使用 COS 默认域名。</p>
 */
public class CosStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(CosStorageService.class);

    /** 腾讯云 COS 客户端。 */
    private final COSClient cosClient;

    /** 存储配置（bucket、region、自定义域名等）。 */
    private final WenshuProperties.Storage storage;

    /**
     * 构造函数。
     *
     * @param cosClient COS 客户端（已完成凭据配置）
     * @param storage   存储配置
     */
    public CosStorageService(COSClient cosClient, WenshuProperties.Storage storage) {
        this.cosClient = cosClient;
        this.storage = storage;
    }

    /**
     * 上传文件到腾讯云 COS。
     *
     * @param objectKey   对象键（路径前缀不含 bucket）
     * @param inputStream 待上传数据流
     * @param contentType MIME 类型
     * @param sizeBytes   文件字节数（-1 表示未知）
     * @return 上传成功的对象键
     */
    @Override
    public String upload(String objectKey, InputStream inputStream,
                         String contentType, long sizeBytes) {
        log.info("[CosStorageService] 开始上传文件 objectKey={} contentType={} sizeBytes={}",
                objectKey, contentType, sizeBytes);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            if (sizeBytes > 0) {
                metadata.setContentLength(sizeBytes);
            }
            PutObjectRequest putRequest = new PutObjectRequest(
                    storage.getBucket(), objectKey, inputStream, metadata);
            cosClient.putObject(putRequest);
            log.info("[CosStorageService] 文件上传成功 objectKey={}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("[CosStorageService] 文件上传失败 objectKey={}", objectKey, e);
            throw new RuntimeException("COS 文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成带时效的预签名下载 URL。
     * 如果配置了自定义域名，则替换 URL 的主机部分。
     *
     * @param objectKey 对象键
     * @param ttl       URL 有效期
     * @return 预签名访问 URL
     */
    @Override
    public String generatePresignedUrl(String objectKey, Duration ttl) {
        log.debug("[CosStorageService] 生成预签名 URL objectKey={} ttlSeconds={}", objectKey, ttl.getSeconds());
        Date expiration = Date.from(Instant.now().plus(ttl));
        GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
                storage.getBucket(), objectKey);
        urlRequest.setExpiration(expiration);
        java.net.URL url = cosClient.generatePresignedUrl(urlRequest);
        String urlStr = url.toString();

        // 自定义域名替换（可选）
        String customDomain = storage.getCustomDomain();
        if (customDomain != null && !customDomain.isBlank()) {
            // 将 COS 默认域名替换为自定义域名（仅替换 host 部分）
            urlStr = urlStr.replaceFirst("https?://[^/]+", customDomain);
        }
        log.debug("[CosStorageService] 预签名 URL 生成完成 objectKey={}", objectKey);
        return urlStr;
    }

    /**
     * COS 凭据已配置，始终返回 true。
     */
    @Override
    public boolean isAvailable() {
        return true;
    }
}
