package com.czx.wenshu.application.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * 文件存储服务端口（P9-04）。
 *
 * <p>抽象对象存储操作，生产环境由腾讯云 COS 实现。
 * 凭据未配置时降级为 {@code NoopFileStorageService}，不影响应用启动。</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>上传文件到对象存储</li>
 *   <li>生成带时效的预签名下载 URL</li>
 *   <li>查询当前存储服务是否可用（凭据是否已配置）</li>
 * </ul>
 */
public interface FileStorageService {

    /**
     * 将输入流上传到对象存储。
     *
     * @param objectKey   对象键（路径），如 {@code exports/{userId}/data.zip}
     * @param inputStream 待上传的数据流
     * @param contentType MIME 类型，如 {@code application/zip}
     * @param sizeBytes   文件字节数（-1 表示未知，但 COS 建议提供）
     * @return 上传后的对象键（与传入相同）
     */
    String upload(String objectKey, InputStream inputStream, String contentType, long sizeBytes);

    /**
     * 为已存储对象生成带时效的预签名下载 URL。
     *
     * @param objectKey 对象键
     * @param ttl       URL 有效期
     * @return 可直接访问的预签名 URL
     */
    String generatePresignedUrl(String objectKey, Duration ttl);

    /**
     * 存储服务是否已配置并可用。
     * 凭据未设置时返回 {@code false}，此时上传操作会抛出 {@link UnsupportedOperationException}。
     *
     * @return true 表示 COS 凭据已配置可用
     */
    boolean isAvailable();
}
