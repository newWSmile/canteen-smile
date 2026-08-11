# Server 微服务工程

Java 17 + Spring Boot 3.5.16 + Spring Cloud 2025.0.3 + Sa-Token 1.45.0（Redis Jackson 持久化）+ MyBatis 3.0.5 + PostgreSQL + Redis 的 Maven 多模块工程。JSON、日志与定时任务标准分别为 Jackson、SLF4J + Logback、XXL-JOB；XXL-JOB 依赖在首个真实任务出现时按已确认版本引入，当前不创建空任务。

## 服务与共享模块

身份、租户、机构、角色、菜单、数据权限及认证会话的已确认业务设计见 `docs/iam-enterprise-design.md`。该文档是领域设计基线，不代表其中的接口、表或权限编码已经实现。

```text
server/
├─ smile-common/                  # 稳定的跨服务响应、错误码与业务异常契约
├─ smile-web-starter/             # Servlet 服务统一异常、TraceId、Validation、Sa-Token
├─ smile-infrastructure-starter/  # MyBatis、Redis Key 与统一有界线程池
├─ smile-gateway/                 # 独立 WebFlux 网关服务，外部唯一入口和粗粒度登录认证
├─ smile-auth-service/            # 认证凭证、验证码、Token 和设备会话
└─ smile-iam-service/             # 租户、机构、用户、角色、菜单和数据权限
```

网关、认证服务和 IAM 服务均是可独立启动、配置和部署的 Spring Boot 应用。IAM 领域已由 `docs/iam-enterprise-design.md` 确认，但真实业务表和接口只能按根目录 `doc/iam-executable-implementation-plan.md` 分阶段实施。新增其它领域时创建 `smile-<domain>-service`，不得把采购、库存、学校、食堂等未确认领域堆入 Auth 或 IAM，也不得为了凑数量创建空服务。

## 服务内部包职责

```text
com.canteen.smile.modules.<module>/
├─ controller/   # REST 接口，仅参数校验与调用 Service
├─ service/      # 业务编排与本地事务边界
├─ mapper/       # MyBatis 数据访问，数据权限下推 SQL
├─ entity/       # 本服务数据库表映射，不跨服务共享
├─ dto/          # API 入参或版本化服务间契约
├─ vo/           # 前端响应
├─ converter/    # DTO、Entity、VO 显式转换
└─ client/       # 服务间或第三方调用边界，真实调用出现后创建
```

数据对象和依赖注入样板代码统一使用 Lombok 的 `@Getter`、`@Setter`、`@RequiredArgsConstructor` 等明确注解；Entity 禁止直接使用 `@Data`，避免自动生成不合适的 `equals`、`hashCode` 和包含敏感字段的 `toString`。静态接口权限优先使用 Sa-Token `@SaCheckPermission`，权限码引用领域内集中维护的常量；只有运行期动态权限集合无法由注解表达时，才允许在统一授权组件中调用编程式校验。

## 微服务边界

- 外部请求只进入 `smile-gateway`。网关负责登录认证和路由级防护，下游服务负责最终接口、角色、组织及数据权限。
- 每个业务服务独立拥有数据，禁止跨服务查库、复用 Mapper/Entity 或使用共享数据库表完成耦合调用。
- 服务间调用使用显式 Client、版本化 DTO、超时与有限重试；只重试幂等操作。跨服务流程优先最终一致性、事件与补偿。
- 网关把 `/api/auth/v1/**` 转发到 `AUTH_SERVICE_URI`，把 `/api/iam/v1/**` 转发到 `IAM_SERVICE_URI`。开发环境分别默认 `http://127.0.0.1:8081` 和 `http://127.0.0.1:8082`，生产环境必须显式提供。内部 `/internal/**` 接口不得经 Gateway 暴露。具体决策见 `docs/microservice-architecture.md`。

## 首位平台管理员安全引导

首次启动前必须通过进程环境或未提交的 `application-local.yml` 为 Auth 配置 `PLATFORM_BOOTSTRAP_SECRET`，并为 Auth 与 IAM 配置完全一致的 `INTERNAL_HMAC_AUTH_TO_IAM_SECRET`。两者都必须使用本地生成的高熵随机值，禁止复制示例值到生产。

三个后端服务启动后，访问平台前端 `/bootstrap`，输入引导密钥、全平台永久用户名和初始密码。成功响应只显示一次恢复码；恢复码应离线保存，仅用于后续账号恢复或明确启用的高风险验证流程，普通登录不会消费。随后前往 `/login` 使用用户名和密码直接建立当前设备会话。首位平台身份激活后，引导接口永久返回冲突，运行环境应立即移除 `PLATFORM_BOOTSTRAP_SECRET`。

平台初始化密码和用户名密码登录均先调用 `/api/auth/v1/password-encryption/challenges` 获取当前轮换 RSA 公钥和一次性短期挑战。浏览器使用 RSA-OAEP-SHA256 包装随机 AES-256 密钥，并使用 AES-GCM 加密密码；`purpose`、`keyId`、`nonce` 和服务端 `timestamp` 共同作为附加认证数据。RSA 密钥由 Auth 启动时预生成，默认 30 天轮换；每次挑战仍使用独立 nonce，默认 120 秒过期并通过 Redis 原子消费一次。该应用层信封不得替代生产环境 HTTPS，生产入口和内部敏感链路仍必须启用 TLS 或 mTLS。

已经实现的外部 Auth 契约见 `docs/openapi/auth-v1.yaml`。当前阶段普通平台登录使用用户名和密码；短信验证码及恢复码不作为每次登录的固定步骤。后续仅在异常设备、高风险操作或专门账号恢复流程中按策略启用二次验证。

## 配置与启动

`.env.example` 只列本机示例变量。可部署服务各自维护 dev、test、prod 配置，并允许开发者使用不提交 Git 的 `application-local.yml` 保存本机或受信任局域网连接信息；生产配置无默认凭据。

本地使用 IDEA 启动时，在对应 Spring Boot Run Configuration 中设置：

```text
SPRING_PROFILES_ACTIVE=local
```

然后分别编辑 `smile-auth-service`、`smile-iam-service` 和 `smile-gateway` 下的 `application-local.yml`。这些文件已被根目录 `.gitignore` 排除，真实密码不得复制到其它可提交配置。IAM 必须使用独立 PostgreSQL 数据库，通过 `IAM_DB_URL`、`IAM_DB_USERNAME`、`IAM_DB_PASSWORD` 配置。

Redis 使用 Cluster 模式。`REDIS_CLUSTER_NODES` 采用逗号分隔的 `host:port` 节点列表；Redis Cluster 仅支持 DB 0，项目不配置 `database`。Gateway 与 Auth Service 必须连接同一集群，才能共享 Sa-Token 登录状态。启用 ACL 时填写 `REDIS_USERNAME`，未启用时保持为空。

```bash
mvn test
mvn package
mvn install
mvn -pl smile-gateway spring-boot:run
mvn -pl smile-auth-service spring-boot:run
mvn -pl smile-iam-service spring-boot:run
```

测试不得用 H2 证明 PostgreSQL 行为；数据库集成测试应使用独立 PostgreSQL 测试实例或 Testcontainers。

## 变更验证范围

后端编译和测试按变更影响范围执行，不要求每次改动都运行整个 `server` 的全量测试：

- 仅调整局部字段、方法参数或方法内部实现，且未改变模块边界、公共契约或整体结构时，至少编译受影响模块，并按需运行直接相关测试。
- 出现明显结构变动、删除整个类、移除整个模块或其它可能产生跨模块影响的变更时，扩大验证范围并运行后端全量相关测试。
- 无论变更大小，都不得完全跳过验证；因环境原因无法验证时，交付说明必须写明未验证内容和原因。

## SQL 迭代

当前不引入 Flyway。Auth 与 IAM 的 DDL、DML 分别集中在根目录 `sql/auth` 和 `sql/iam`，脚本命名、冻结、执行顺序及记录规则以根目录 `sql/README.md` 为准。禁止在各服务 `src/main/resources` 下维护第二套 SQL，已在共享环境执行的脚本只能通过新增更高编号脚本修正。

## IAM Outbox 与 XXL-JOB

IAM 的可靠事件由处理器 `iamOutboxDeliveryJob` 有界投递。部署时设置 `XXL_JOB_ENABLED=true`、`XXL_JOB_ADMIN_ADDRESSES` 和通过安全配置注入的 `XXL_JOB_ACCESS_TOKEN`，然后在 XXL-JOB 管理端创建同名 Bean 模式任务。任务可按秒级或业务允许的延迟配置；多实例通过 PostgreSQL `FOR UPDATE SKIP LOCKED` 竞争领取，不得为每个实例创建不同处理器。失败事件按配置指数退避，达到最大次数进入 `DEAD`，需配置监控告警并在排除原因后受控重投。

本地或新环境启用投递前，按 `sql/CHANGELOG.md` 顺序执行 `IAM_DDL_0007` 和 `IAM_DML_0002`。Auth 的 `/internal/auth/v1/security-events` 只接受内部 HMAC 请求，不通过 Gateway 暴露；重复事件按事件 ID 和载荷摘要幂等返回成功。

## 短信发送策略

短信供应商统一通过 Auth 的 `SmsClient` 策略接口接入。当前 `local`、`dev` 和 `test` 环境提供 `LOCAL_DATABASE_LOG` 策略：它不连接真实运营商，统一发送服务会先用请求 ID 原子防重，再保存脱敏手机号、业务用途、可读正文快照和投递结果，并输出不含完整手机号、验证码、Token 或一次性链接的安全日志。正文快照保持明文可读，但验证码等一次性秘密统一替换为 `******`。

本地启用前先执行 `AUTH_DDL_0007`、`AUTH_DDL_0008` 和 `AUTH_DDL_0009`，并通过未提交的 `application-local.yml` 或环境变量配置：

```text
SMS_PROVIDER_CODE=LOCAL_DATABASE_LOG
SMS_MOBILE_HASH_PEPPER=本地生成的高熵随机值
SMS_CODE_HASH_PEPPER=本地生成的高熵随机值（未单独设置时可显式回退到手机号 Pepper）
AUTH_MOBILE_ENCRYPTION_KEY_ID=local-v1
AUTH_MOBILE_ENCRYPTION_KEY=Base64 编码的 32 字节随机密钥
```

平台端短信记录查询经 Gateway 进入 IAM，由 IAM 完成 `platform:sms-delivery:view` 权限校验后通过 HMAC Client 调用 Auth，禁止 IAM 跨库读取。完整手机号只作为精确查询条件在 Auth 内存中计算 HMAC 摘要，响应始终只返回脱敏号码。平台菜单权限由 `IAM_DML_0004` 发布。后续接入真实厂商时新增对应 `SmsClient` 实现并切换 `SMS_PROVIDER_CODE`，不得修改验证码业务服务或在 Controller 中直接调用厂商 SDK。

短信挑战通过 `POST /api/auth/v1/sms/challenges` 创建：默认五分钟有效，同一手机号的重发锁及手机号小时、每日额度按 `LOGIN`、`MOBILE_BIND` 等业务用途隔离；来源 IP 和设备的小时、每日额度保持跨用途总控，禁止切换用途绕过安全限流。这些阈值由 Auth 自有全局短信策略控制。Redis Cluster 的多维限流 Key 使用固定哈希槽标签执行单次 Lua 原子检查；全部 Key 均设置过期时间且只包含业务用途以及手机号、IP、设备摘要。验证码只保存绑定挑战、用途和手机号摘要的 HMAC，默认错误五次后失效，成功校验时由具体登录、找回或绑定流程原子消费一次。

当前登录租户账号可以通过 `/api/auth/v1/mobile/binding` 查询脱敏绑定状态，通过受登录保护的挑战和确认接口完成首次绑定。确认时 Auth 会再次核对提交手机号与挑战摘要，随后使用 AES-256-GCM 随机 IV 加密完整手机号，并只在界面和审计中返回脱敏号码。同一账号只能存在一个有效绑定，同一手机号摘要允许关联多个不同账号；换绑属于后续独立敏感流程，不能复用首次绑定接口覆盖原记录。

租户端手机号登录先调用 `/api/auth/v1/sms/challenges` 创建 `LOGIN` 用途挑战，再调用 `/api/auth/v1/login/sms` 原子消费验证码。Auth 仅按手机号摘要查询绑定账号 ID，并通过 HMAC Client 批量请求 IAM 复核账号、租户、机构、有效期和会话策略；单账号直接创建会话，多账号返回按最近登录排序的安全候选与五分钟一次性选择票据。最终选择调用 `/api/auth/v1/login/account-selection`，Auth 会重新计算候选集合摘要并原子消费票据，禁止篡改账号或重放。

平台短信管理分为“短信列表、短信设置、短信安全”。执行 `IAM_DML_0005` 后，平台端可维护验证码有效期、错误次数和手机号/IP/设备限流；修改必须填写原因并通过当前平台密码再认证。验证码明文留存默认关闭，显式开启后只影响后续新投递记录；完整手机号和应用日志无论开关状态都必须脱敏。
