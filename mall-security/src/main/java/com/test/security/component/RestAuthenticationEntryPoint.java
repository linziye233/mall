package com.test.security.component;

import cn.hutool.json.JSONUtil;
import com.test.mall.common.api.CommonResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义返回结果：未登录或登录过期
 * 实现AuthenticationEntryPoint接口，处理未认证访问
 * 将默认的HTML错误页面改为RESTful JSON响应
 * Created by macro on 2018/5/14.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    /**
     * 未认证时的处理方法
     * 当用户未登录或Token失效访问受保护资源时自动调用
     * 返回HTTP状态码401和统一的JSON格式错误信息
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param authException 认证异常信息
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(JSONUtil.parse(CommonResult.unauthorized(authException.getMessage())));
        response.getWriter().flush();
    }
}
