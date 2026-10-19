package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.sql.*;

/**
 * Servlet xử lý đăng nhập cho quản trị viên.
 * Hiện tại hỗ trợ cơ chế tự động đăng nhập bằng tài khoản mẫu để thuận tiện cho việc kiểm thử.
 */
@WebServlet("/admin/login")
public class AdminLoginServlet extends HttpServlet {

    // Thông tin tài khoản quản trị mẫu
    private static final String DEMO_PHONE = "0365365113";
    private static final String DEMO_PASSWORD = "1";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Kiểm tra nếu đã đăng nhập thì chuyển thẳng vào trang Dashboard
        HttpSession sess = req.getSession(false);
        if (sess != null && sess.getAttribute("adminUser") != null) {
            resp.sendRedirect(req.getContextPath() + "/admin");
            return;
        }

        // Thực hiện tự động đăng nhập
        try {
            if (authenticate(req, DEMO_PHONE, DEMO_PASSWORD)) {
                resp.sendRedirect(req.getContextPath() + "/admin");
                return;
            }
            // Cơ chế dự phòng: Nếu không tìm thấy user trong DB, vẫn cho phép vào với quyền mặc định
            sess = req.getSession(true);
            sess.setAttribute("adminUser", "Quản trị viên");
            sess.setAttribute("adminId", "NV002");
            resp.sendRedirect(req.getContextPath() + "/admin");
        } catch (SQLException e) {
            // Trường hợp lỗi cơ sở dữ liệu, cho phép truy cập khu vực admin để không làm gián đoạn kiểm thử
            sess = req.getSession(true);
            sess.setAttribute("adminUser", "Quản trị viên");
            sess.setAttribute("adminId", "NV002");
            resp.sendRedirect(req.getContextPath() + "/admin");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Xử lý POST tương tự GET cho đơn giản
        doGet(req, resp);
    }

    /**
     * Xác thực thông tin đăng nhập từ cơ sở dữ liệu.
     */
    private boolean authenticate(HttpServletRequest req, String phone, String password) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT ma_nv, ten_nv FROM NhanVien WHERE sdt=? AND mat_khau=?")) {
            p.setString(1, phone != null ? phone.trim() : "");
            p.setString(2, password != null ? password : "");
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return false;
                // Lưu thông tin nhân viên vào Session
                HttpSession sess = req.getSession(true);
                sess.setAttribute("adminUser", r.getString("ten_nv"));
                sess.setAttribute("adminId", r.getString("ma_nv"));
                return true;
            }
        }
    }
}
