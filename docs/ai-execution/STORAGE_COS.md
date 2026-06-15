# Tencent COS Storage Plan

对象存储统一使用腾讯云 COS，覆盖原始技术文档中的 MinIO 方案。

## Maven Dependency

项目使用腾讯云 COS XML Java SDK：

```xml
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
    <version>${cos.sdk.version}</version>
</dependency>
```

当前版本在 `pom.xml` 中配置为 `5.6.269`。

## Configuration

配置前缀：`wenshu.storage`

```yaml
wenshu:
  storage:
    provider: cos
    region: ${TENCENT_COS_REGION:ap-guangzhou}
    bucket: ${TENCENT_COS_BUCKET:wenshu-1250000000}
    secret-id: ${TENCENT_COS_SECRET_ID:}
    secret-key: ${TENCENT_COS_SECRET_KEY:}
    custom-domain: ${TENCENT_COS_CUSTOM_DOMAIN:}
    presigned-url-ttl-minutes: ${TENCENT_COS_PRESIGNED_URL_TTL_MINUTES:60}
```

`bucket` 必须使用 COS 完整 bucket 名，格式通常为 `bucket-name-appid`。

## Infrastructure Classes To Implement

建议后续实现以下类：

- `domain.storage.FileStorageService`：领域侧端口。
- `infrastructure.storage.CosStorageService`：COS 适配器。
- `infrastructure.config.CosStorageConfig`：创建 `COSClient` Bean。
- `application.export.ExportService`：导出文件后上传 COS 并返回预签名 URL。

## Required Capabilities

- 上传导出文件：DOCX、FDX、分镜表、ZIP。
- 上传用户导入源文件：TXT、DOCX。
- 生成临时下载 URL：默认 60 分钟。
- 支持可选自定义域名：`customDomain` 不为空时，下载链接可替换为业务域名。
- 缺少 SecretId/SecretKey 时应用必须能启动；真正调用上传/下载时返回明确业务错误。

## Object Key Convention

统一对象路径：

| Type | Object Key |
| --- | --- |
| Script export | `exports/{userId}/{draftId}/{filename}` |
| Data export ZIP | `exports/{userId}/data/{taskId}.zip` |
| Import source | `imports/{userId}/{projectId}/{parseId}/{filename}` |
| User avatar | `avatars/{userId}/{filename}` |

## Acceptance Criteria

- 不再新增 MinIO 依赖、配置、Docker 服务或文档任务。
- `docker-compose.yml` 只负责本地 PostgreSQL 和 Redis。
- 导出任务完成后返回 COS 预签名 URL。
- COS 凭证缺失时，启动测试仍通过。
- 真实 COS 凭证可用时，上传、下载、预签名 URL 能完成端到端验证。
