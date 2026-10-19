package com.footballbooking.controller;
import com.footballbooking.config.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
@WebServlet("/admin/invoice-management")
public class InvoiceManagementServlet extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try (Connection c = DBConnection.getConnection()) {
            String search = q.getParameter("search");
            String status = q.getParameter("status");
            int page = Math.max(1, parseInt(q.getParameter("page"), 1));
            int size = 10;
            StringBuilder sql = new StringBuilder("SELECT hd.ma_hd, kh.ho_ten, kh.sdt, s.ten_san, l.ngay_dat, l.ca, l.gio_bat_dau, l.gio_ket_thuc, hd.tong_tien, hd.tien_coc, hd.trang_thai FROM HoaDon hd JOIN KhachHang kh ON hd.ma_kh=kh.ma_kh JOIN CTHD d ON d.ma_hd=hd.ma_hd JOIN LichSan l ON l.ma_lich=d.ma_lich JOIN San s ON s.ma_san=l.ma_san WHERE 1=1");
            List<String> args = new ArrayList<>();
            if (search != null && !search.isEmpty()) { sql.append(" AND (hd.ma_hd LIKE ? OR kh.ho_ten LIKE ?)"); args.add("%"+search+"%"); args.add("%"+search+"%"); }
            if (status != null && !status.isEmpty()) { sql.append(" AND hd.trang_thai = ?"); args.add(status); }
            List<Map<String, Object>> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                bind(ps, args); ResultSet rs = ps.executeQuery();
                while (rs.next()) rows.add(mapRow(rs));
            }
            q.setAttribute("rows", rows);
            q.setAttribute("totalPages", Math.max(1, (count(c, sql.toString().replace("SELECT hd.ma_hd, kh.ho_ten, kh.sdt, s.ten_san, l.ngay_dat, l.ca, l.gio_bat_dau, l.gio_ket_thuc, hd.tong_tien, hd.tien_coc, hd.trang_thai FROM HoaDon hd JOIN KhachHang kh ON hd.ma_kh=kh.ma_kh JOIN CTHD d ON d.ma_hd=hd.ma_hd JOIN LichSan l ON l.ma_lich=d.ma_lich JOIN San s ON s.ma_san=l.ma_san WHERE 1=1"), args) + size - 1) / size);
            q.setAttribute("page", page);
            q.getRequestDispatcher("/jsp/admin/invoice-management.jsp").forward(q, p);
        } catch (Exception e) { throw new ServletException(e); }
    }
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try (Connection c = DBConnection.getConnection()) {
            String op = q.getParameter("op");
            String id = q.getParameter("id");
            if ("cancel".equals(op)) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE HoaDon SET trang_thai=N'ĐÃ HỦY' WHERE ma_hd=?")) {
                    ps.setString(1, id); ps.executeUpdate();
                }
            }
            p.sendRedirect(q.getContextPath() + "/admin/invoice-management");
        } catch (Exception e) { throw new ServletException(e); }
    }
    private int count(Connection c, String countSql) throws SQLException {
        int total = 0;
        try (Statement s = c.createStatement()) {
            String cs = "SELECT COUNT(*) FROM (" + countSql + ") t";
            ResultSet rs = s.executeQuery(cs);
            if (rs.next()) total = rs.getInt(1);
        }
        return total;
    }
    private void bind(PreparedStatement ps, List<String> a) throws SQLException { for (int i = 0; i < a.size(); i++) ps.setString(i+1, a.get(i)); }
    private int parseInt(String s, int d) { try { return Integer.parseInt(s); } catch (Exception e) { return d; } }
    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ma_hd", rs.getString("ma_hd"));
        m.put("ho_ten", rs.getString("ho_ten"));
        m.put("sdt", rs.getString("sdt"));
        m.put("ten_san", rs.getString("ten_san"));
        m.put("ngay_dat", rs.getDate("ngay_dat"));
        m.put("ca", rs.getString("ca"));
        m.put("gio_bat_dau", rs.getTime("gio_bat_dau"));
        m.put("gio_ket_thuc", rs.getTime("gio_ket_thuc"));
        m.put("tong_tien", rs.getBigDecimal("tong_tien"));
        m.put("tien_coc", rs.getBigDecimal("tien_coc"));
        m.put("trang_thai", rs.getString("trang_thai"));
        return m;
    }
}