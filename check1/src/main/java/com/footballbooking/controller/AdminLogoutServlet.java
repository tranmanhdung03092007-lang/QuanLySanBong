package com.footballbooking.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * Servlet xử lý đăng xuất cho quản trị viên.
 * Xóa thông tin Session và chuyển hướng về trang đăng nhập.
 */
@WebServlet("/admin/logout")
public class AdminLogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Hủy bỏ session hiện tại nếu có
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // Chuyển hướng về trang đăng nhập
        resp.sendRedirect(req.getContextPath() + "/admin/login");
    }
}
