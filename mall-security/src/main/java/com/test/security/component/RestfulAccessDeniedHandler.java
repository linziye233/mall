package com.test.security.component;

import cn.hutool.json.JSONUtil;
import com.test.mall.common.api.CommonResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义返回结果：没有权限访问时
 * 实现AccessDeniedHandler接口，处理权限不足的情况
 * 将默认的错误页面改为RESTful JSON响应
 * Created by macro on 2018/4/26.
 */
public class RestfulAccessDeniedHandler implements AccessDeniedHandler{
    /**
     * 访问被拒绝时的处理方法
     * 当用户已登录但权限不足时自动调用
     * 返回HTTP状态码403和统一的JSON格式错误信息
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param e 访问拒绝异常信息
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException e) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(JSONUtil.parse(CommonResult.forbidden(e.getMessage())));
        response.getWriter().flush();
    }
}
