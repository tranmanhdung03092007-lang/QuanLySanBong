package com.footballbooking.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        HttpSession s = q.getSession(false);
        if (s == null || s.getAttribute("adminId") == null) { p.sendRedirect(q.getContextPath() + "/admin/login"); return; }
        try {
            java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
            stats.put("Tổng HĐ", new AdminCrudServlet().count("HoaDon"));
            stats.put("Chưa TT", new AdminCrudServlet().countWhere("HoaDon", "trang_thai", "CHƯA THANH TOÁN"));
            stats.put("Đã TT", new AdminCrudServlet().countWhere("HoaDon", "trang_thai", "ĐÃ THANH TOÁN"));
            stats.put("Sân", new AdminCrudServlet().count("San"));
            stats.put("KH", new AdminCrudServlet().count("KhachHang"));
            q.setAttribute("stats", stats);
        } catch (Exception e) { q.setAttribute("stats", new java.util.LinkedHashMap<>()); }
        q.getRequestDispatcher("/jsp/admin/dashboard.jsp").forward(q, p);
    }
}