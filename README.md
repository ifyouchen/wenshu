# wenshu

文枢 wenshu 是一套面向长篇创作者的 AI 写作工作台，核心能力包括可控长篇续写、分级润色、上下文锚点、一致性审查、小说转剧本、全文搜索替换和写作统计。

## Current Scope

本仓库当前落地的是后端 V1 项目底座：

- Spring Boot 3 + Java 21
- DDD 四层包结构：`interfaces`、`application`、`domain`、`infrastructure`
- PostgreSQL 16 + pgvector、Redis 7、腾讯云 COS
- Flyway 初始数据库迁移
- 统一 API 返回结构和全局异常处理
- 系统健康探针：`GET /api/v1/system/health`

## Run Locally

确保 Maven 使用 JDK 21。当前机器上可用的路径是 `F:\jdk21`：

```powershell
$env:JAVA_HOME='F:\jdk21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

COS 需要通过环境变量配置：

```powershell
$env:TENCENT_COS_REGION='ap-guangzhou'
$env:TENCENT_COS_BUCKET='wenshu-1250000000'
$env:TENCENT_COS_SECRET_ID='your-secret-id'
$env:TENCENT_COS_SECRET_KEY='your-secret-key'
```

启动后访问：

- API health: `http://localhost:8080/api/v1/system/health`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`

## Test Profile

单元测试默认使用 `test` profile，数据源为 H2 内存库，Flyway 关闭，Redis 仓储关闭，因此不需要本地 PostgreSQL、Redis 或 COS 凭证即可运行：

```powershell
$env:JAVA_HOME='F:\jdk21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn test
```

## Documentation

产品、技术和交互设计原始文档位于 `docs/`。

- 项目分析：`docs/PROJECT_ANALYSIS.md`
- AI 执行文档入口：`docs/ai-execution/README.md`
- 原始文档覆盖矩阵：`docs/ai-execution/COVERAGE_MATRIX.md`
- AI 后续续跑指南：`docs/ai-execution/CONTINUATION_GUIDE.md`
- 实现进度台账：`docs/ai-execution/PROGRESS.md`
