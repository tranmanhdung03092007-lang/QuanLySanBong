package com.footballbooking.controller;
import com.footballbooking.config.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.*;
@WebServlet("/admin/login")
public class AdminLoginServlet extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        q.setAttribute("sdt", "0365365113");
        q.getRequestDispatcher("/jsp/admin/login.jsp").forward(q, p);
    }
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws IOException {
        String sdt = q.getParameter("sdt");
        String pw = q.getParameter("password");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT ma_nv, ten_nv FROM NhanVien WHERE sdt=? AND mat_khau=?")) {
            ps.setString(1, sdt); ps.setString(2, pw);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                HttpSession s = q.getSession();
                s.setAttribute("adminId", rs.getString("ma_nv"));
                s.setAttribute("adminUser", rs.getString("ten_nv"));
                p.sendRedirect(q.getContextPath() + "/admin"); return;
            }
        } catch (Exception e) {}
        q.setAttribute("error", "Sai tai khoan hoac mat khau");
        q.setAttribute("sdt", sdt);
        q.getRequestDispatcher("/jsp/admin/login.jsp").forward(q, q.getResponse());
    }
}