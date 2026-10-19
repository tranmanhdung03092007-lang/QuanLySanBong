package com.footballbooking.controller;
import com.footballbooking.config.DBConnection;
import com.footballbooking.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
@WebServlet("/api/admin/invoices/*")
public class AdminInvoiceApiServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String search = req.getParameter("search") != null ? req.getParameter("search") : "";
        String status = req.getParameter("status") != null ? req.getParameter("status") : "";
        try {
            Map<String, Object> result = getInvoices(search, status);
            PrintWriter out = resp.getWriter();
            out.print(JsonUtil.stringify(result));
            out.flush();
        } catch (SQLException e) {
            resp.setStatus(500);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            PrintWriter out = resp.getWriter();
            out.print(JsonUtil.stringify(error));
            out.flush();
        }
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        try {
            if (pathInfo != null && pathInfo.endsWith("/approve")) {
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/approve", "");
                approveInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã xác nhận chuyển khoản thành công");
            } else if (pathInfo != null && pathInfo.endsWith("/reject")) {
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/reject", "");
                rejectInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã từ chối thanh toán");
            } else if (pathInfo != null && pathInfo.endsWith("/processing")) {
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/processing", "");
                setProcessingInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã chuyển sang trạng thái đang xử lý");
            }
        } catch (SQLException e) {
            resp.setStatus(500);
            sendErrorResponse(resp, e.getMessage());
        }
    }
    private Map<String, Object> getInvoices(String search, String status) throws SQLException {
        List<Map<String, Object>> invoices = new ArrayList<>();
        Map<String, Integer> stats = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT hd.ma_hd, hd.ma_kh, kh.ho_ten, kh.sdt, s.ten_san, ls.ngay_dat, hd.tong_tien, hd.tien_coc, hd.trang_thai, hd.ngay_lap FROM HoaDon hd JOIN KhachHang kh ON hd.ma_kh = kh.ma_kh JOIN LichSan ls ON hd.ma_lich = ls.ma_lich JOIN San s ON ls.ma_san = s.ma_san WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (!search.isEmpty()) {
            sql.append(" AND (hd.ma_hd LIKE ? OR kh.ho_ten LIKE ? OR kh.sdt LIKE ?)");
            String p = "%" + search + "%";
            params.add(p); params.add(p); params.add(p);
        }
        if (!status.isEmpty()) { sql.append(" AND hd.trang_thai = ?"); params.add(status); }
        sql.append(" ORDER BY hd.ngay_lap DESC");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setString(i + 1, params.get(i));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> invoice = new LinkedHashMap<>();
                invoice.put("ma_hd", rs.getString("ma_hd"));
                invoice.put("ma_kh", rs.getString("ma_kh"));
                invoice.put("ho_ten", rs.getString("ho_ten"));
                invoice.put("sdt", rs.getString("sdt"));
                invoice.put("ten_san", rs.getString("ten_san"));
                invoice.put("ngay_dat", rs.getDate("ngay_dat"));
                invoice.put("tong_tien", rs.getInt("tong_tien"));
                invoice.put("tien_coc", rs.getInt("tien_coc"));
                invoice.put("trang_thai", rs.getString("trang_thai"));
                invoice.put("ngay_lap", rs.getDate("ngay_lap"));
                invoices.add(invoice);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("invoices", invoices);
        return result;
    }
    private void approveInvoice(String invoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE HoaDon SET trang_thai = 'ĐÃ THANH TOÁN' WHERE ma_hd = ?")) {
            stmt.setString(1, invoiceId);
            stmt.executeUpdate();
        }
    }
    private void setProcessingInvoice(String invoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE HoaDon SET trang_thai = 'ĐANG XỬ LÝ' WHERE ma_hd = ? AND trang_thai = 'CHƯA THANH TOÁN'")) {
            stmt.setString(1, invoiceId);
            stmt.executeUpdate();
        }
    }
    private void rejectInvoice(String invoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE HoaDon SET trang_thai = 'ĐÃ HỦY' WHERE ma_hd = ?")) {
            stmt.setString(1, invoiceId);
            stmt.executeUpdate();
        }
    }
    private void sendSuccessResponse(HttpServletResponse resp, String message) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", message);
        PrintWriter out = resp.getWriter();
        out.print(JsonUtil.stringify(response));
        out.flush();
    }
    private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("success", "false");
        response.put("error", message);
        PrintWriter out = resp.getWriter();
        out.print(JsonUtil.stringify(response));
        out.flush();
    }
}