mall-security 模块功能说明
基于代码分析，mall-security 是电商系统的安全认证与授权模块，提供统一的安全防护能力。
📌 核心功能
JWT Token 认证
生成、验证和刷新 JWT Token
支持 Token 过期时间管理
实现 Token 自动刷新机制（30分钟内可复用）
请求权限控制
自定义白名单路径配置（无需认证的接口）
所有其他请求强制身份认证
支持跨域 OPTIONS 请求
无状态会话管理
禁用 Session，采用 STATELESS 模式
每次请求通过 JWT Token 验证用户身份
异常处理
未认证访问的统一响应（RestAuthenticationEntryPoint）
权限不足的统一响应（RestfulAccessDeniedHandler）

🔧 关键技术点
技术
用途
Spring Security
安全框架基础，提供认证授权能力
JWT (jjwt)
无状态 Token 生成与解析，算法为 HS512
BCryptPasswordEncoder
密码加密存储，单向哈希
OncePerRequestFilter
JWT 过滤器，每个请求拦截验证
ConfigurationProperties
白名单路径动态配置

🏗️ 架构设计亮点
独立通用模块：打包为 jar，供其他微服务（如 mall-gateway、mall-member）依赖复用
可扩展设计：
UserDetailsService 由子类注入，支持不同服务的用户查询逻辑
注释提到后续改为 Feign 调用 Member 服务获取用户信息
Token 刷新策略：
Token 未过期时可刷新
30 分钟内重复刷新返回原 Token，避免频繁更新
白名单配置化：通过 secure.ignored.urls 灵活配置公开接口


1. Spr1. Spring Security vs. 手写过滤器
   维度
   Spring Security
   手写过滤器 (Filter/Interceptor)
   核心本质
   一个由一连串过滤器链组成的庞大框架。
   简单的 Servlet Filter 或 Spring Interceptor。
   功能全面性
   全家桶：认证、授权、CSRF 防护、CORS、Session 管理、OAuth2、OIDC 等。
   单一职责：通常只处理 Token 校验或简单的权限判断。
   代码复杂度
   初始配置较复杂（如你看到的 SecurityConfig），学习曲线陡峭。
   起步简单，几行代码就能实现拦截。
   安全性
   极高。经过全球开发者十几年的验证，能防御各种底层攻击。
   较低。容易遗漏细节（如忘记处理 OPTIONS 请求、路径遍历漏洞等）。
   维护成本
   低。标准化配置，换人也容易接手。
   高。随着业务变复杂，过滤器里会堆满 if-else，变成“屎山”。
2. 为什么很多旧项目或简单项目喜欢“手写”？
   你以前看到的项目自己写过滤器，通常是因为以下原因：
   历史包袱：在 Spring Boot 和 Spring Security 4.x/5.x 普及之前，Spring Security（前身 Acegi）配置极其繁琐，很多老项目宁愿自己写。
   微服务架构的演变：在早期的微服务实践中，有些团队选择在**网关层（Gateway）**统一做鉴权，而内部的服务为了追求极致的性能或简化配置，就只写一个简单的过滤器来解析 Header 里的用户信息。
   对 JWT 的支持：早期的 Spring Security 对无状态的 JWT 支持不够“开箱即用”，开发者觉得写一个 JwtFilter 更直观。
3. 引入 Spring Security 的核心优势是什么？
   虽然 Mall 项目看起来只是用了它来做 JWT 校验，但它带来了几个手写很难做到的好处：
   统一的异常处理入口： 正如你之前问的 RestAuthenticationEntryPoint。在手写过滤器中，你需要在每个地方手动 response.setStatus(401) 并返回 JSON。而在 Spring Security 中，你可以集中定义“未登录”、“没权限”、“Token 过期”分别怎么处理。
   与 Spring 生态无缝集成： 你可以直接在 Controller 方法参数里写 @AuthenticationPrincipal User user，Spring Security 会自动把当前登录用户塞给你。手写过滤器通常需要你自己搞一个 ThreadLocal 来存用户信息。
   细粒度的权限控制： 如果你想实现“只有 ADMIN 角色才能访问 /product/delete”，Spring Security 只需要一行注解 @PreAuthorize("hasRole('ADMIN')")。手写的话，你得写一个复杂的 AOP 切面去解析注解。
   安全性兜底： 它会默认帮你处理很多安全细节，比如防止点击劫持、强制 HTTPS 等。
4. 业内的最佳实践是什么？
   目前主流的方案是 “组合拳”：
   方案 A（Mall 项目采用的方式）： Spring Security + JWT Filter。利用 Spring Security 的管理能力（上下文、异常处理、权限注解），同时自定义一个 JWT 过滤器插入到它的链条中。这是目前最稳健的做法。
   方案 B（大型微服务）： Gateway（网关鉴权） + Spring Security（内部辅助）。在网关层拦截大部分非法请求，内部服务依然开启 Spring Security，但配置为“仅校验本地 Session 或内部 Token”，确保即使绕过网关，服务本身也是安全的。
   总结
   如果是个人小项目或原型：手写过滤器没问题，快且灵活。
   如果是企业级项目、涉及金钱交易或用户隐私：必须使用 Spring Security。因为它不仅仅是一个拦截器，更是一套完整的安全解决方案。
   Mall 项目作为一个电商系统，涉及会员、订单、支付，使用 Spring Security 是为了保证系统的健壮性和可扩展性。