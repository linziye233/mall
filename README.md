# mall
第一阶段：基础设施与公共模块 🔧
1. mall-common （公共模块）
   为什么先做这个：所有其他模块都依赖它
   包含内容：
   通用返回结果封装（CommonResult）
   统一异常处理
   常量定义
   通用工具类
   学习目标：理解项目的统一规范
2. mall-mbg （MyBatis Generator代码生成器）
   为什么做这个：快速生成DAO层代码
   包含内容：
   MyBatis Generator配置
   数据库表对应的Entity、Mapper、XML
   学习目标：掌握代码生成技巧

第二阶段：核心业务模块（从简单到复杂） 📦
3. mall-product （商品服务）⭐ 推荐第一个业务模块
   推荐理由：
   业务逻辑相对独立
   不涉及复杂的分布式事务
   包含Zookeeper分布式锁实战
   缓存策略典型（Redis + Zookeeper）
   核心技术：
   Spring Boot + MyBatis
   Redis缓存
   Zookeeper分布式锁
   Nacos服务注册
   学习价值：⭐⭐⭐⭐⭐
4. mall-member （会员服务）
   推荐理由：
   用户认证授权的基础
   包含JWT令牌生成
   相对简单的CRUD操作
   核心技术：
   Spring Security
   JWT认证
   MongoDB（可选）
   学习价值：⭐⭐⭐⭐

第三阶段：认证与网关 🔐
5. mall-authcenter （认证中心）
   为什么这时候做：需要member模块的用户体系
   包含内容：
   OAuth2.0认证服务器
   JWT令牌签发
   用户授权管理
   核心技术：
   Spring Security OAuth2
   JWT
   JKS密钥管理
   学习价值：⭐⭐⭐⭐⭐
6. mall-gateway （API网关）
   为什么这时候做：需要authcenter提供认证功能
   包含内容：
   路由转发
   统一鉴权
   限流熔断
   核心技术：
   Spring Cloud Gateway
   Sentinel限流
   JWT验证
   学习价值：⭐⭐⭐⭐⭐

第四阶段：复杂业务模块 🛒
7. mall-order （订单服务）
   推荐理由：
   业务复杂度较高
   涉及分布式事务
   消息队列应用
   核心技术：
   RocketMQ异步消息
   Seata分布式事务
   分库分表（ShardingSphere）
   Redis分布式ID生成
   学习价值：⭐⭐⭐⭐⭐
8. mall-seckill （秒杀服务）
   推荐理由：
   高并发场景实战
   多级缓存架构
   流量削峰
   核心技术：
   Redis预减库存
   RabbitMQ/RocketMQ削峰
   限流降级
   分布式锁
   学习价值：⭐⭐⭐⭐⭐
9. mall-search （搜索服务）
   推荐理由：
   Elasticsearch实战
   数据同步机制
   核心技术：
   Elasticsearch
   Canal数据同步
   全文检索
   学习价值：⭐⭐⭐⭐

第五阶段：高级特性模块 🎁
10. mall-coupons （优惠券服务）
    营销业务实现
    规则引擎应用
11. mall-canal （数据同步服务）
    MySQL binlog监听
    缓存自动更新
12. mall-portal （门户服务）
    前端接口聚合
    支付集成
13. mall-admin （后台管理服务）
    运营管理功能
    数据统计报表
