# HeyTrip 酒店供应商集成系统
## 全流程开发实践提示词规则

> 本文档旨在为 HeyTrip 酒店供应商集成系统的研发、测试、运维人员提供一套可复用的提示词（Prompt）规则，帮助团队在需求澄清、架构设计、编码实现、测试验证、上线运维等全过程中，与智能助手协作时保持一致的沟通套路和交付标准。

---

## 1. 适用范围与目标

- **适用人员**：需求分析师、后端工程师、测试工程师、运维工程师、技术写作者。
- **覆盖阶段**：需求澄清 → 架构设计 → 数据建模 → 开发实施 → 安全与合规 → 测试验证 → 部署上线 → 监控运维 → 文档交付。
- **项目背景**：Spring Boot 3.2 + Java 17 微服务，集成多酒店供应商，核心模块包括供应商适配器、静态/动态数据服务、订单与报价 API、系统配置中心、安全认证与监控。

---

## 2. 全流程任务分解

| 阶段 | 关键目标 | 推荐输出 | 关联目录/文件 |
| :-- | :-- | :-- | :-- |
| 需求澄清 | 明确业务场景与验收标准 | 需求问答、边界条件 | `docs/需求记录/` |
| 架构设计 | 评估影响面、设计方案 | 架构草图、影响分析 | `src/main/java/com/heytrip/hotel/supplier/**` |
| 数据建模 | 定义实体与存储策略 | ER 说明、SQL 变更 | `entity/`、`repository/`、`docs/sql/` |
| 开发实施 | 编写/调整业务代码 | 任务清单、实现说明 | `controller/`、`adapter/`、`service/` |
| 安全合规 | 校验签名、权限、敏感信息 | 安全检查清单 | `config/`、`filter/`、`utils/SignUtil.java` |
| 测试验证 | 单元&集成测试、数据回归 | 测试计划、用例列表 | `src/test/java/com/heytrip/hotel/supplier/**` |
| 部署上线 | 构建镜像与配置灰度 | 部署计划、回滚策略 | `deploy/`、`pom.xml` |
| 监控运维 | 健康检查、日志、告警 | 运维操作手册 | `/actuator/**`、`logs/` |
| 文档交付 | 使用说明、变更记录 | README 更新、发布说明 | `README.md`、`docs/**` |

---

## 3. 提示词规则（按阶段）

以下提示词可直接复制后替换尖括号占位符使用，或作为自定义提示的模板。

### 3.1 需求澄清

**目标**：明确业务背景、验收标准、边界和依赖。

- **核心提示结构**：
  ```text
  需求背景：<业务场景>
  当前问题：<痛点或缺陷>
  期望目标：<量化目标或验收标准>
  依赖模块：controller、adapter、service、repository 等
  需确认的问题：
  1. …
  2. …
  请帮我列出待确认清单，并补充可能遗漏的边界情况。
  ```
- **使用场景**：对接新的供应商能力、扩展订单流程、调整认证规则。

### 3.2 架构设计与影响分析

**目标**：评估变更影响范围，制定设计方案。

- **提示模板**：
  ```text
  当前任务：<描述任务>
  涉及模块：
  - controller/<…>
  - adapter/<impl>
  - service/<…>
  约束条件：Spring Boot 3.2、Java 17、现有 SupplierAdapter 架构
  请输出：
  1. 影响范围列表（代码、配置、数据库、部署）
  2. 推荐设计方案（含优缺点）
  3. 风险点与缓解措施
  ```
- **注意事项**：引用 `SupplierAdapterManager`、`SecurityFilter`、`ConfigController` 等关键类时，强调接口兼容性与线程安全。

### 3.3 数据建模与迁移

**目标**：设计/调整实体、仓储与数据库脚本。

- **提示模板**：
  ```text
  需求：新增/调整 <实体名>
  现有实体：`entity/<Entity>.java`
  现有表结构：参考 `docs/sql/*.sql`
  请生成：
  - 实体字段设计（含注释、索引、默认值）
  - JPA Repository 方法建议
  - SQL 迁移脚本草案
  - 回滚方案
  ```
- **补充**：强调使用 `@Comment`、`@Index` 等注解风格与现有代码保持一致。

### 3.4 开发实施

**目标**：生成或调整业务实现代码。

- **提示模板**：
  ```text
  背景：实现 <功能描述>
  涉及层级：
  - controller: <Controller 名>
  - service: <Service 或 Adapter>
  - repository: <Repository>
  需要注意：
  - SupplierAdapter 接口兼容
  - Reactor Mono/Flux 超时与错误处理
  - Caffeine 缓存策略
  请输出开发计划（分解任务 + 验证方式），并补充代码片段或伪代码。
  ```
- **编码规范提醒**：
  - 使用 `Result<T>` 作为公共返回结构。
  - 日志使用 `logger.info/debug/warn/error`，包含关键上下文。
  - 认证相关操作调用 `AuthHelper.requireAuthentication` 或 `logAuthInfo`。

### 3.5 安全与合规

**目标**：确保签名认证、权限控制、敏感信息处理符合规范。

- **提示模板**：
  ```text
  任务：评估 <接口/模块> 的安全性
  参考配置：`application.yml` 中 app.security、authorization
  核查要点：
  1. Header 校验（app、timestamp、sign）
  2. 白名单与受限路径
  3. 日志敏感信息脱敏
  请列出风险清单与修复建议，如需调整 `SecurityFilter` 或 `SecurityConfig`，给出改动思路。
  ```

### 3.6 测试验证

**目标**：设计和执行单元、集成、性能测试。

- **提示模板**：
  ```text
  场景：<功能/缺陷描述>
  依赖数据：<必要的 SQL / CSV>
  需要的测试：
  - 单元测试（列出类与方法）
  - 集成测试（Mock Supplier、外部 API）
  - 性能/并发验证（引用 `deploy/api_concurrent_test.sh`）
  请生成测试计划，包含：用例编号、前置条件、输入、预期输出、验证脚本。
  ```
- **回归重点**：供应商健康检查、订单状态流转、静态数据同步。

### 3.7 部署上线

**目标**：制定部署步骤、环境变量配置与回滚策略。

- **提示模板**：
  ```text
  发布版本：<版本号>
  部署方式：Docker Compose / JAR + Shell / 其他
  相关文件：`deploy/docker-compose.yml`、`deploy/deploy-jar.sh`
  请生成部署 Runbook：
  1. 构建命令
  2. 环境变量清单（含敏感信息处理方式）
  3. 健康检查验证步骤
  4. 回滚策略
  ```
- **强调**：使用 `HEALTHCHECK`、`Actuator`、日志挂载路径 `/app/logs` 等细节。

### 3.8 监控与运维

**目标**：运行期观测、告警与故障排查。

- **提示模板**：
  ```text
  目标：监控 <模块/指标>
  参考：`/actuator/health|metrics`、日志路径 `logs/`
  请输出：
  - 关键指标（健康、吞吐、错误率、连接池）
  - 告警阈值建议
  - 故障自检步骤（含常见 5xx、认证失败、数据库连接异常）
  ```

### 3.9 文档与交付

**目标**：更新 README、发布说明、接口文档。

- **提示模板**：
  ```text
  变更摘要：<描述>
  影响范围：接口 / 配置 / 部署 / 文档
  需要更新的文档：`README.md`、`docs/<…>.md`
  请整理交付物清单：
  - 新增/修改章节
  - 使用示例或 API 样例
  - 版本记录
  ```
- **风格约束**：Markdown 二级标题起，表格与列表对齐，引用 KaTeX 用于公式。

---

## 4. 基本原则与通用提示规范

### 4.1 基本原则

1. 严格遵循 SOLID（单一职责、开闭原则、里氏替换、接口隔离、依赖倒置）设计原则，确保模块职责清晰、易扩展。
2. 严格遵守 DRY（不要重复自己）原则，尽量复用已有能力、工具类与通用逻辑。
3. 严格执行 KISS（保持简单）原则，优先选择易理解的实现，避免过度设计与过早抽象。
4. 严格践行 YAGNI（你不会需要它）原则，仅实现当前真实需求，避免臆想未来场景导致的复杂化。
5. 全流程遵循 OWASP 安全最佳实践，重点防范 SQL 注入、XSS、CSRF 等常见漏洞，敏感数据按需脱敏。
6. 采用清晰的分层架构，保持 controller、service、adapter、repository、config 等层次职责分明、松耦合。
7. 代码、注释、文档优先使用中文（必要技术术语可保留英文），保持团队沟通一致性。
8. 与智能助手交互时尽量使用中文描述问题与上下文，技术术语除外，以提升理解准确度。

### 4.2 通用提示词规范

1. **上下文充足**：补充相关类、配置、数据库脚本路径，避免笼统问题。
2. **显式期望**：明确要 AI 输出的文档类型（计划表、代码片段、测试用例、风险分析等）。
3. **结构化结果**：要求对方使用表格、列表、代码块，让结果易于落地执行。
4. **引用现有规范**：提及项目惯例，例如 `Result<T>`、`SupplierAdapter`、`AuthHelper`、`Caffeine` 等关键字。
5. **闭环验证**：在提示中要求列出验证步骤、工具命令（如 Maven、Docker、Actuator）。

---

## 5. 附加资源引用指南

- **代码参考**：优先查阅 `README.md`、`adapter/`、`controller/`、`config/`、`filter/`、`repository/` 、`service/`  目录。
- **配置中心**：`application.yml` 中的 `app.*`、`spring.*`、`management.*` 段。
- **部署脚手架**：`deploy/docker-compose.yml`、`deploy/Dockerfile`、`deploy/deploy-jar.sh`。
- **数据脚本**：`docs/sql/basic.sql`、`docs/sql/hotel.sql`。
- **测试骨架**：`src/test/java/com/heytrip/hotel/supplier/**`。
- **文档范式**：`docs/供应商对接说明文档.md`、`docs/HttpClient使用指南.md`。

---

## 6. 使用示例

> 示例：准备为新增供应商编写开发计划。

```text
背景：新增供应商 X，需要支持报价、订单、静态数据。
涉及模块：
- adapter/impl/
- controller/PricingController.java
- adapter/service/SupplierApiService.java
约束：沿用 SupplierAdapter 接口、支持健康检查、需通过 MD5 签名认证。
请输出：
1. 影响分析（代码、配置、数据库、部署）
2. 详细开发步骤与检查项
3. 推荐测试用例（含 Mock Supplier & 并发测试）
```

---

## 7. 维护与迭代建议

- 定期回顾提示词是否覆盖新引入的模块（如新增供应商、异地多活、OAuth 支持等）。
- 当大版本升级 Spring Boot、Java 或数据库时，补充相应的兼容性检查提示。
- 保持文档版本号与发布节奏一致，可在项目根 README 中同步链接。

---

**版本**：v1.0.0  
**维护责任人**：HeyTrip 开发团队 Pax
