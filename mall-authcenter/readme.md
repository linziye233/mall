# Mall AuthCenter - 认证中心模块

## 📋 模块概述
**mall-authcenter** 是整个微服务架构中的统一认证授权中心，基于 Spring Security OAuth2 + JWT 实现，专门负责为网关提供用户身份认证和令牌颁发服务。

---


🔑 授权中心参与的完整流程
流程1：用户登录获取 Token


步骤1: 前端发送登录请求
POST http://mall-authcenter:9999/oauth/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic bWFsbC13ZWI6c2VjcmV0MTIz  (base64编码的 clientId:clientSecret)

Body:
grant_type=password&username=zhangsan&password=123456

↓

步骤2: AuthServerConfig 处理请求
- JdbcClientDetailsService 验证 clientId 和 clientSecret
- UserDetailService 验证用户名和密码
- TokenEnhancer 添加自定义信息（memberId、nickName等）
- JwtAccessTokenConverter 生成 JWT Token

↓

步骤3: 返回 Token
{
"access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
"token_type": "Bearer",
"refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
"expires_in": 3599,
"scope": "read write"
}

流程2：携带 Token 访问业务接口

步骤1: 前端携带 Token 请求订单服务
GET http://mall-gateway:8080/api/orders/123
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

↓

步骤2: 网关校验 Token（两种方式）

方式A: 网关本地校验（推荐，性能更好）
- 使用 JWT 公钥本地解析 Token
- 验证签名是否有效
- 检查是否过期
- 无需调用 authcenter

方式B: 调用 authcenter 校验
POST http://mall-authcenter:9999/oauth/check_token
Body: token=eyJhbGci...
返回: { active: true, user_name: "zhangsan", ... }

↓

步骤3: 网关转发请求到订单服务
- 将用户信息放入请求头
- X-User-Id: 123
- X-Username: zhangsan

↓

步骤4: 订单服务处理业务逻辑

## 🗂️ 文件结构说明

### 1. **核心配置类**

#### `AuthServerConfig.java` - 授权服务器配置
- **作用**：OAuth2 授权服务器的核心配置
- **关键功能**：
    - 使用 `@EnableAuthorizationServer` 启用授权服务器
    - 配置 JWT Token 存储方式（`JwtTokenStore`）
    - 从 JKS 证书文件加载 RSA 密钥对用于 JWT 签名
    - 配置客户端详情服务（`JdbcClientDetailsService`），从数据库表 `oauth_client_details` 读取客户端信息
    - 自定义 Token 增强器链，包含自定义业务信息和 JWT 转换器
    - 配置 Token 校验访问权限

#### `WebSecurityConfig.java` - Web 安全配置
- **作用**：Spring Security 的基础安全配置
- **关键功能**：
    - 配置用户认证管理器（`AuthenticationManager`）
    - 集成自定义 `UserDetailService` 和密码编码器（`BCryptPasswordEncoder`）
    - 放行静态资源（`/assets/**`, `/css/**`, `/images/**`）
    - 提供密码加密工具方法（main 方法可用于生成加密密码）

#### `MyBatisConfig.java` - MyBatis 配置
- **作用**：MyBatis ORM 框架配置

---

### 2. **JWT 相关组件**

#### `TokenEnhancer.java` - JWT 自定义增强器
- **作用**：向 JWT Token 中注入自定义业务数据
- **关键功能**：
    - 将会员 ID、昵称、积分等非敏感信息添加到 Token 的 `additionalInfo` 中
    - 避免后续业务查询用户信息时的数据库访问
    - ⚠️ **注意**：注释明确提示不应添加敏感字段

#### `JwtCAProperties.java` - JWT 证书配置属性
- **作用**：通过 `@ConfigurationProperties` 绑定 JWT 证书相关配置
- **配置项**：
    - `keyPairName`：JKS 证书文件路径
    - `keyPairAlias`：证书别名
    - `keyPairSecret`：证书私钥密码
    - `keyPairStoreSecret`：证书存储密钥

---

### 3. **用户认证服务**

#### `UserDetailService.java` - 用户详情加载服务
- **作用**：实现 Spring Security 的 `UserDetailsService` 接口
- **关键功能**：
    - 根据用户名从数据库加载用户信息
    - 参数校验（用户名为空时抛出异常）
    - 日志记录（警告和成功日志）
    - 通过 `UmsMemberMapper` 查询会员表

#### `MemberDetails.java` - 会员详情封装
- **作用**：实现 Spring Security 的 `UserDetails` 接口
- **关键功能**：
    - 封装 `UmsMember` 对象
    - 实现权限返回（当前硬编码为 `TEST` 权限）
    - 账户状态判断（根据 `status` 字段判断是否启用）
    - 其他账户状态默认返回 `true`（未过期、未锁定等）

---

### 4. **配置文件**

#### `application.yml` - 应用配置
- **数据源配置**：MySQL 数据库连接 + Druid 连接池监控
- **Nacos 服务发现**：注册到 `tl.nacos.com:8848`
- **JWT 证书配置**：指向 `jwt.jks` 文件及密码
- **服务端口**：9999

#### `bootstrap.yml` - 启动配置
- **Nacos 配置中心**：
    - 加载共享配置：`tulingmall-db-common.yml`、`tulingmall-nacos.yml`
    - 支持配置动态刷新
- **日志级别**：设置 Nacos 客户端日志为 `warn`

#### `jwt.jks` - JWT 签名证书
- **作用**：RSA 非对称加密密钥对，用于 JWT Token 的签名和验证

---

## 🔧 核心技术点

### 1. **OAuth2 + JWT 认证架构**


- 采用 **JWT 无状态令牌**，适合微服务分布式场景
- 使用 **RSA 非对称加密**签名，提高安全性
- 支持 **OAuth2 标准协议**，兼容多种客户端授权模式

### 2. **自定义 Token 增强**
- 通过 `TokenEnhancerChain` 组合多个增强器
- 在 Token 中嵌入业务数据（memberId、nickName、integration）
- 减少后续业务服务的数据库查询压力

### 3. **客户端配置持久化**
- 使用 `JdbcClientDetailsService` 从数据库读取客户端配置
- 需要预先在 `oauth_client_details` 表中配置客户端信息
- 支持动态管理第三方客户端，无需重启服务

### 4. **Nacos 集成**
- **服务注册与发现**：其他服务可通过 Nacos 找到认证中心
- **配置中心**：集中管理数据库、中间件等配置
- **配置动态刷新**：修改配置后自动生效

### 5. **安全性设计**
- 密码使用 `BCryptPasswordEncoder` 加密存储
- JWT 证书密码通过配置文件管理（建议生产环境使用密钥管理服务）
- Token 校验需要携带 `clientId` 和 `clientSecret`
- 静态资源放行，动态接口严格鉴权

---

## ⚠️ 注意事项与潜在问题

### 1. **安全问题**
- ❌ **证书密码明文存储**：`application.yml` 中 JWT 证书密码为明文，生产环境应使用加密或密钥管理服务
- ❌ **硬编码权限**：`MemberDetails` 中权限固定为 `TEST`，应根据实际业务动态分配角色权限
- ⚠️ **Token 暴露过多信息**：虽然注释提醒不添加敏感字段，但 memberId 等信息仍可能被解析，建议评估风险

### 2. **代码质量问题**
- ⚠️ **异常处理不完整**：`UserDetailService` 中查询不到用户时仅记录日志，未抛出异常，可能导致空指针
- ⚠️ **缺少参数校验**：未对用户名进行长度、格式等校验
- ⚠️ **日志敏感信息**：日志中打印完整用户对象，可能泄露密码等敏感信息

### 3. **性能优化**
- 💡 **数据库查询优化**：`getByUsername` 方法可添加缓存（如 Redis）减少数据库压力
- 💡 **Token 刷新机制**：未看到 refresh_token 相关配置，建议实现令牌刷新避免频繁登录

### 4. **依赖版本**
- ⚠️ **OAuth2 版本较旧**：使用 `spring-cloud-starter-oauth2:2.2.5.RELEASE`，该版本已停止维护
- 💡 **建议迁移**：考虑升级到 Spring Authorization Server（Spring 官方新一代授权服务器）

### 5. **配置管理**
- ⚠️ **数据库配置暴露**：`application.yml` 中数据库账号密码为明文且是 root 权限，存在安全风险
- 💡 **建议使用**：Nacos 配置中心统一管理敏感配置，并启用配置加密

---

## 🚀 典型使用流程

### 客户端获取 Token


---

## 📦 依赖关系
- **Spring Boot**：基础框架
- **Spring Security OAuth2**：认证授权框架
- **JWT (jjwt)**：令牌生成与解析
- **MyBatis**：数据库访问
- **Nacos**：服务注册与配置中心
- **Druid**：数据库连接池与监控
- **mall-mbg**：内部模块，提供 Mapper 和 Model

---

## 🎯 总结
`mall-authcenter` 是一个功能完整的 OAuth2 认证中心，采用成熟的 JWT 技术方案，集成了服务注册、配置管理等微服务基础设施。但在安全性、代码健壮性和技术选型上仍有优化空间，建议根据实际业务需求进行改进。