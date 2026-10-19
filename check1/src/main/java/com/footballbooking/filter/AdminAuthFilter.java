package com.footballbooking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Filter bảo vệ các tài nguyên trong khu vực quản trị (/admin/*).
 * Kiểm tra quyền truy cập của người dùng trước khi cho phép vào các trang quản lý.
 */
@WebFilter(urlPatterns = {"/admin/*"})
public class AdminAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Cho phép truy cập không cần đăng nhập vào trang login và logout
        String servletPath = req.getServletPath();
        if ("/admin/login".equals(servletPath) || "/admin/logout".equals(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Kiểm tra session xem admin đã đăng nhập chưa
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("adminUser") == null) {
            // Nếu chưa đăng nhập, chuyển hướng về trang đăng nhập
            resp.sendRedirect(req.getContextPath() + "/admin/login");
            return;
        }
        
        // Đã đăng nhập, cho phép đi tiếp
        chain.doFilter(request, response);
    }
}
