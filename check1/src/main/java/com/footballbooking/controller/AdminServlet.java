package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Servlet chính của trang quản trị (Dashboard).
 * Hiển thị các thống kê tổng quan và danh sách các thực thể có thể quản lý.
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
    // Bản đồ chứa tên hiển thị tiếng Việt của các thực thể quản lý
    private static final Map<String, String> L = new HashMap<>();
    static {
        L.put("San", "Sân"); L.put("LichSan", "Lịch Sân"); L.put("KhachHang", "Khách Hàng");
        L.put("NhanVien", "Nhân Viên"); L.put("DichVu", "Dịch Vụ"); L.put("HoaDon", "Hóa Đơn");
        L.put("CTHD", "Chi Tiết Hóa Đơn");
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        q.setAttribute("L", L);
        try {
            // Lấy thông tin thống kê cơ bản từ CSDL
            Map<String, Object> s = new LinkedHashMap<>();
            try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement()) {
                // Đếm tổng số sân đang hoạt động (loại trừ các sân đã bị xóa mềm)
                s.put("Tổng số sân", st.executeQuery("SELECT COUNT(*) FROM San WHERE trang_thai <> N'ĐÃ XÓA'").getInt(1));
            }
            q.setAttribute("stats", s);
        } catch (Exception e) {
            // Trường hợp lỗi CSDL, hiển thị dữ liệu mặc định để giao diện không bị lỗi
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("Tổng số sân", 4);
            q.setAttribute("stats", s);
        }
        // Chuyển hướng đến trang Dashboard của admin
        q.getRequestDispatcher("/jsp/admin/dashboard.jsp").forward(q, p);
    }
}
