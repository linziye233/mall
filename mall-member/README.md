1. Spring Security：本地的“保安”
   Spring Security 是一个技术框架，它通常运行在每一个微服务内部。
   职责：它负责看守当前这个服务的门。它会检查每个进来的请求：“你有通行证吗？你的通行证是真的吗？你有权限进这个房间吗？”
   特点：它是去中心化的。如果你在订单服务、会员服务都引入了 Spring Security，那么它们各自都有一套“保安”在干活。
   局限性：如果每个服务都有自己的用户表和密码逻辑，那用户在 A 服务登录了，去 B 服务还得再登一次，这就不是真正的微服务了。
2. 鉴权中心（Auth Center）：统一的“派出所”
   鉴权中心（在你的项目中是 mall-authcenter）是一个独立的微服务。
   职责：它负责统一发放和管理通行证（Token）。它不直接看守业务服务的门，但它告诉所有的“保安”：“这张证是我发的，是真的，这个人确实是张三。”
   特点：它是中心化的。它实现了 SSO（单点登录）。用户只需要在这里登录一次，拿到 Token，就可以拿着这个 Token 去访问会员、订单、商品等所有服务。
   核心技术：通常会结合 OAuth2 和 JWT 协议。

它们是如何配合工作的？
在你的 Mall 项目中，它们的分工如下：
步骤
动作
谁在执行？
1. 登录
   用户输入账号密码，请求登录。
   鉴权中心 (验证密码，生成 JWT Token)
2. 发令牌
   返回一个加密的 JWT 字符串给前端。
   鉴权中心
3. 访问业务
   前端带着 Token 访问“会员服务”。
   前端 -> 会员服务
4. 验令牌
   会员服务的 Spring Security 拦截请求，用公钥解密 Token，确认用户身份。
   Spring Security (在会员服务内)
5. 授权
   Spring Security 判断该用户是否有权限执行某个操作（比如修改资料）。
   Spring Security

为什么有了 Spring Security 还要写一个鉴权中心？
解耦（最重要的原因）： 如果不抽离出鉴权中心，你的会员服务、订单服务都得知道怎么查数据库、怎么比对密码。一旦密码加密算法要改，你得改所有服务。现在只需要改鉴权中心一个地方。
单点登录 (SSO)： 你肯定不希望用户在逛淘宝时，从“我的淘宝”切换到“购物车”时又让他重新登录一遍。鉴权中心确保了“一次登录，处处通行”。
安全性： 业务服务（如会员、订单）不需要直接暴露数据库里的密码字段。只有鉴权中心接触密码，其他服务只认 Token。
总结
Spring Security 是一套工具/标准，用来在 Java 代码层面实现安全控制（认证+授权）。
鉴权中心 是一个架构设计/服务，用来统一管理整个系统的用户身份。
在你的 mall-member 代码中： 你会发现它其实没有完整引入 Spring Security 的依赖来做本地登录，而是通过 RestTemplate 远程调用 mall-authcenter。这说明在这个架构里，鉴权中心负责“发证”，而会员服务目前只负责“调接口拿证”。

1. 拦截与提取：JwtAuthenticationTokenFilter.java
   这个类继承了 OncePerRequestFilter，意味着每个请求进来时它都会先跑一遍。
   第 63-65 行：它会从 HTTP 请求头（Header）中提取 Authorization 字段。通常前端传过来的格式是 Bearer eyJhbGciOi...。它会去掉 Bearer 前缀，拿到那串长长的 JWT Token。
   第 68 行：如果当前用户还没被认证（SecurityContextHolder 里没有信息），它就会开始干活。
2. 校验与解析：JwtTokenUtil.java
   这是“保安”用来查验身份证真伪的工具包。
   第 91-99 行：getUserNameFromToken 方法会利用密钥（secret）去解密 Token。如果解密成功，就能拿到里面的用户名（sub 字段）。
   第 110-113 行：validateToken 会做两件事：一是确认 Token 里的用户名和数据库里的一致；二是检查 Token 有没有过期（isTokenExpired）。
3. 建立上下文：回到过滤器
   第 71-74 行：一旦校验通过，Spring Security 就会创建一个 UsernamePasswordAuthenticationToken 对象。
   关键点：它把这个对象放进了 SecurityContextHolder。这一步非常重要！ 之后在你的 Controller 或 Service 里，无论你在哪个线程（主线程）里调用 SecurityContextHolder.getContext().getAuthentication()，都能拿到当前登录用户的信息。

总结：它与鉴权中心的区别
特性
鉴权中心 (Auth Center)
Spring Security (本地)
动作
发证：验证密码，生成 Token。
查证：解析 Token，确认身份。
发生时间
用户点击“登录”按钮时。
用户访问每一个受保护接口时。
依赖
依赖数据库（查用户、查密码）。
依赖密钥（jwt.secret）进行验签。
目的
解决“你是谁”的问题。
解决“我能让你进这个门吗”的问题。
一个有趣的细节
在 JwtAuthenticationTokenFilter.java 的第 34 行，有一个 UserDetailsService。 你会发现这里有个矛盾：既然 Token 已经能解析出用户名了，为什么还要去查数据库加载 UserDetails？
这通常是为了获取用户的权限列表（Authorities）。虽然 Token 里存了用户名，但为了安全起见，或者为了获取最新的角色信息（比如用户刚被封号，或者刚被赋予了管理员权限），Spring Security 习惯在每次请求时重新从数据库（或缓存）拉取一次用户的最新状态。