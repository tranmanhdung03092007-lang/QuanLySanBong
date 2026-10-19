package com.footballbooking.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
@WebServlet("/admin/logout")
public class AdminLogoutServlet extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws IOException {
        q.getSession().invalidate();
        p.sendRedirect(q.getContextPath() + "/admin/login");
    }
}