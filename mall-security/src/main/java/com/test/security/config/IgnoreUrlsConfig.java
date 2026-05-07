package com.test.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于配置不需要保护的资源路径
 * 通过@ConfigurationProperties绑定配置文件中的secure.ignored.urls
 * 支持动态配置白名单，无需修改代码即可调整公开接口
 * Created by macro on 2018/11/5.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "secure.ignored")
public class IgnoreUrlsConfig {

    /**
     * 白名单路径列表
     * 这些路径不需要JWT认证即可访问
     * 例如：登录接口、注册接口、Swagger文档等
     *
     * 配置示例（application.yml）：
     * secure:
     *   ignored:
     *     urls:
     *       - /api/member/login
     *       - /api/member/register
     *       - /swagger-ui.html
     */
    private List<String> urls = new ArrayList<>();

}
