package com.test.security.config;

import com.test.security.component.JwtAuthenticationTokenFilter;
import com.test.security.component.RestAuthenticationEntryPoint;
import com.test.security.component.RestfulAccessDeniedHandler;
import com.test.security.util.JwtTokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * 对SpringSecurity的配置的扩展，支持自定义白名单资源路径和查询用户逻辑
 * Created by macro on 2019/11/5.
 */
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 配置HTTP安全策略
     * 定义请求授权规则、会话管理、异常处理和过滤器链
     *
     * @param httpSecurity HTTP安全构建器
     * @throws Exception 配置异常
     */
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry = httpSecurity
                .authorizeRequests();
        for (String url : ignoreUrlsConfig().getUrls()) {
            registry.antMatchers(url).permitAll();
        }
        //允许跨域请求的OPTIONS请求
        registry.antMatchers(HttpMethod.OPTIONS).permitAll();
        //任何请求需要身份认证
        registry.and()
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                // 关闭跨站请求防护及不使用session
                .and()
                .csrf()
                .disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // 自定义权限拒绝处理类
                .and()
                .exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler())
                .authenticationEntryPoint(restAuthenticationEntryPoint())
                // 自定义权限拦截器JWT过滤器
                .and()
                .addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 配置认证管理器
     * 设置用户详情服务和密码编码器，用于用户登录认证
     *
     * @param auth 认证管理器构建器
     * @throws Exception 配置异常
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder());
    }

    /**
     * 创建密码编码器Bean
     * 使用BCrypt强哈希算法对密码进行加密存储
     *
     * @return BCryptPasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 创建JWT认证过滤器Bean
     * 用于拦截请求并验证JWT Token的有效性
     *
     * @return JwtAuthenticationTokenFilter实例
     */
    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() {
        return new JwtAuthenticationTokenFilter();
    }

    /**
     * 暴露认证管理器Bean
     * 供外部服务（如登录接口）使用，用于验证用户名和密码
     *
     * @return AuthenticationManager实例
     * @throws Exception 创建异常
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * 创建访问拒绝处理器Bean
     * 当用户已登录但权限不足时，返回403 JSON响应
     *
     * @return RestfulAccessDeniedHandler实例
     */
    @Bean
    public RestfulAccessDeniedHandler restfulAccessDeniedHandler() {
        return new RestfulAccessDeniedHandler();
    }

    /**
     * 创建未认证入口处理器Bean
     * 当用户未登录或Token失效时，返回401 JSON响应
     *
     * @return RestAuthenticationEntryPoint实例
     */
    @Bean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    /**
     * 创建白名单配置Bean
     * 从配置文件读取不需要认证的路径列表
     *
     * @return IgnoreUrlsConfig实例
     */
    @Bean
    public IgnoreUrlsConfig ignoreUrlsConfig() {
        return new IgnoreUrlsConfig();
    }

    /**
     * 创建JWT工具类Bean
     * 提供Token生成、验证和刷新功能
     *
     * @return JwtTokenUtil实例
     */
    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil();
    }

    /**
     * 测试方法：演示密码加密
     * 将明文密码"123456"使用BCrypt加密后输出
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        //密码加密方式
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        System.out.println(bCryptPasswordEncoder.encode("123456"));
    }
}
