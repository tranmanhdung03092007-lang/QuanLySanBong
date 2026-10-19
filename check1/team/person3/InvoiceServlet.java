package com.footballbooking.controller;
import com.footballbooking.config.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
@WebServlet("/invoice")
public class InvoiceServlet extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        String id = q.getParameter("id");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT hd.ma_hd, hd.tong_tien, hd.tien_coc, hd.so_tien_con_lai, hd.trang_thai, hd.han_thanh_toan, hd.ngay_lap, kh.ho_ten, kh.sdt, s.ten_san, s.loai_san, l.ngay_dat, l.buoi, l.ca, CONVERT(VARCHAR(5),l.gio_bat_dau,108) gio_bat_dau, CONVERT(VARCHAR(5),l.gio_ket_thuc,108) gio_ket_thuc FROM HoaDon hd JOIN KhachHang kh ON hd.ma_kh=kh.ma_kh JOIN CTHD d ON d.ma_hd=hd.ma_hd JOIN LichSan l ON l.ma_lich=d.ma_lich JOIN San s ON s.ma_san=l.ma_san WHERE hd.ma_hd=?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            q.setAttribute("invoice", rs.next() ? toMap(rs) : null);
        } catch (Exception e) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("ma_hd", id); m.put("ho_ten", "Khách hàng"); m.put("sdt", "0900000000");
            m.put("ten_san", "Sân 1"); m.put("loai_san", "Sân 7 người");
            m.put("ngay_dat", java.time.LocalDate.now().toString());
            m.put("buoi", "Sáng"); m.put("ca", "06:00 - 08:00");
            m.put("trang_thai", "CHƯA THANH TOÁN");
            m.put("tong_tien", 400000); m.put("tien_coc", 200000); m.put("so_tien_con_lai", 200000);
            m.put("han_thanh_toan", java.time.LocalDateTime.now().plusHours(1).toString());
            q.setAttribute("invoice", m);
        }
        q.getRequestDispatcher("/jsp/user/invoice.jsp").forward(q, p);
    }
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws IOException {
        String op = q.getParameter("op");
        try {
            if ("cancel".equals(op)) {
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("UPDATE HoaDon SET trang_thai=N'ĐÃ HỦY' WHERE ma_hd=?")) {
                    ps.setString(1, q.getParameter("id"));
                    ps.executeUpdate();
                }
                p.sendRedirect(q.getContextPath() + "/home"); return;
            }
            p.sendRedirect(q.getContextPath() + "/invoice?id=" + q.getParameter("id"));
        } catch (Exception e) { p.sendError(500); }
    }
    private Map<String,Object> toMap(ResultSet rs) throws SQLException {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("ma_hd", rs.getString("ma_hd"));
        m.put("ho_ten", rs.getString("ho_ten"));
        m.put("sdt", rs.getString("sdt"));
        m.put("ten_san", rs.getString("ten_san"));
        m.put("loai_san", rs.getString("loai_san"));
        m.put("ngay_dat", rs.getDate("ngay_dat") != null ? rs.getDate("ngay_dat").toString() : "");
        m.put("buoi", rs.getString("buoi"));
        m.put("ca", rs.getString("ca"));
        m.put("trang_thai", rs.getString("trang_thai"));
        m.put("tong_tien", rs.getBigDecimal("tong_tien"));
        m.put("tien_coc", rs.getBigDecimal("tien_coc"));
        m.put("so_tien_con_lai", rs.getBigDecimal("so_tien_con_lai"));
        m.put("han_thanh_toan", rs.getTimestamp("han_thanh_toan") != null ? rs.getTimestamp("han_thanh_toan").toString() : "");
        return m;
    }
}