package com.cat.standalone.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简化的认证拦截器
 *
 * 简化版本：不再进行Token验证，允许所有请求通过
 * 适用于本地开发和演示环境
 */
@Component
public class SimpleAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 简化：不再验证Token，允许所有请求通过
        // 如需恢复认证，取消以下注释并实现Token验证逻辑
        // String token = request.getHeader("X-Cat-Token");
        // if (!SimpleAuthController.validateToken(token)) {
        //     response.setContentType("application/json;charset=UTF-8");
        //     response.setStatus(401);
        //     response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
        //     return false;
        // }

        return true;
    }
}