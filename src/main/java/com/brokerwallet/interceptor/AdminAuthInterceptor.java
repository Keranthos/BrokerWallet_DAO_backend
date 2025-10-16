package com.brokerwallet.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员认证拦截器
 * 拦截所有需要管理员权限的请求，验证Session中是否存在已登录的管理员信息
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_SESSION_KEY = "ADMIN_USER";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 处理OPTIONS预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取Session
        HttpSession session = request.getSession(false);
        
        // 检查Session是否存在以及是否包含管理员信息
        if (session == null || session.getAttribute(ADMIN_SESSION_KEY) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录或登录已过期，请重新登录\"}");
            return false;
        }

        // 验证通过，可以从Session中获取管理员信息
        // Object adminInfo = session.getAttribute(ADMIN_SESSION_KEY);
        
        return true;
    }
}

