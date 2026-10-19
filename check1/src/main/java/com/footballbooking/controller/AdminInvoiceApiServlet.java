package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import com.footballbooking.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

/**
 * API Servlet cung cấp các điểm cuối (endpoints) JSON để quản lý hóa đơn.
 * Sử dụng chủ yếu cho các yêu cầu AJAX từ giao diện quản trị.
 */
@WebServlet("/api/admin/invoices/*")
public class AdminInvoiceApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Lấy các tham số tìm kiếm và lọc
        String search = req.getParameter("search") != null ? req.getParameter("search") : "";
        String status = req.getParameter("status") != null ? req.getParameter("status") : "";
        String date = req.getParameter("date") != null ? req.getParameter("date") : "";
        
        try {
            // Lấy danh sách hóa đơn dựa trên tiêu chí lọc
            Map<String, Object> result = getInvoices(search, status, date);
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            // Xử lý các hành động cập nhật trạng thái hóa đơn qua URL
            if (pathInfo != null && pathInfo.endsWith("/approve")) {
                // Phê duyệt hóa đơn (Xác nhận đã thanh toán)
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/approve", "");
                approveInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã xác nhận thanh toán thành công");
            } else if (pathInfo != null && pathInfo.endsWith("/reject")) {
                // Từ chối hóa đơn (Hủy hóa đơn)
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/reject", "");
                rejectInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã từ chối thanh toán");
            } else if (pathInfo != null && pathInfo.endsWith("/processing")) {
                // Chuyển sang trạng thái đang xử lý
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/processing", "");
                setProcessingInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã chuyển sang trạng thái đang xử lý");
            } else {
                resp.setStatus(400);
                sendErrorResponse(resp, "Yêu cầu không hợp lệ");
            }
        } catch (SQLException e) {
            resp.setStatus(500);
            sendErrorResponse(resp, e.getMessage());
        }
    }

    /**
     * Truy vấn danh sách hóa đơn kèm theo thống kê số lượng theo trạng thái.
     */
    private Map<String, Object> getInvoices(String search, String status, String date) throws SQLException {
        List<Map<String, Object>> invoices = new ArrayList<>();
        Map<String, Integer> stats = new HashMap<>();
        
        // Truy vấn danh sách hóa đơn chi tiết
        StringBuilder sql = new StringBuilder(
            "SELECT hd.ma_hd, hd.ma_kh, kh.ho_ten, kh.sdt, s.ten_san, " +
            "ls.ngay_dat, hd.tong_tien, hd.tien_coc, hd.trang_thai, hd.ngay_lap " +
            "FROM HoaDon hd " +
            "JOIN KhachHang kh ON hd.ma_kh = kh.ma_kh " +
            "JOIN CTHD c ON c.ma_hd = hd.ma_hd " +
            "JOIN LichSan ls ON ls.ma_lich = c.ma_lich " +
            "JOIN San s ON ls.ma_san = s.ma_san " +
            "WHERE 1=1"
        );

        List<String> params = new ArrayList<>();

        if (!search.isEmpty()) {
            sql.append(" AND (hd.ma_hd LIKE ? OR kh.ho_ten LIKE ? OR kh.sdt LIKE ?)");
            String searchParam = "%" + search + "%";
            params.add(searchParam);
            params.add(searchParam);
            params.add(searchParam);
        }

        if (!status.isEmpty()) {
            sql.append(" AND hd.trang_thai = ?");
            params.add(status);
        }

        if (!date.isEmpty()) {
            sql.append(" AND CAST(hd.ngay_lap AS DATE) = ?");
            params.add(date);
        }

        sql.append(" ORDER BY hd.ngay_lap DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }

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

        // Truy vấn thống kê tổng hợp số lượng hóa đơn theo từng trạng thái
        String statsSql = "SELECT trang_thai, COUNT(*) as count FROM HoaDon GROUP BY trang_thai";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(statsSql)) {

            int total = 0;
            int pending = 0;
            int processing = 0;
            int paid = 0;

            while (rs.next()) {
                String state = rs.getString("trang_thai");
                int count = rs.getInt("count");
                total += count;

                if ("CHƯA THANH TOÁN".equals(state)) {
                    pending = count;
                } else if ("ĐANG XỬ LÝ".equals(state)) {
                    processing = count;
                } else if ("ĐÃ THANH TOÁN".equals(state)) {
                    paid = count;
                }
            }

            stats.put("total", total);
            stats.put("pending", pending);
            stats.put("processing", processing);
            stats.put("paid", paid);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("invoices", invoices);
        result.put("stats", stats);

        return result;
    }

    /**
     * Phê duyệt hóa đơn và cập nhật trạng thái lịch sân tương ứng thành 'ĐÃ ĐẶT'.
     */
    private void approveInvoice(String invoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE HoaDon SET trang_thai = 'ĐÃ THANH TOÁN' WHERE ma_hd = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, invoiceId);
                    stmt.executeUpdate();
                }

                // Đồng bộ trạng thái cho các lịch sân liên quan
                String updateScheduleSql = "UPDATE LichSan SET trang_thai = 'ĐÃ ĐẶT' WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd = ?)";
                try (PreparedStatement scheduleStmt = conn.prepareStatement(updateScheduleSql)) {
                    scheduleStmt.setString(1, invoiceId);
                    scheduleStmt.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Đánh dấu hóa đơn đang trong quá trình xử lý.
     */
    private void setProcessingInvoice(String invoiceId) throws SQLException {
        String sql = "UPDATE HoaDon SET trang_thai = 'ĐANG XỬ LÝ' WHERE ma_hd = ? AND trang_thai = 'CHƯA THANH TOÁN'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, invoiceId);
            stmt.executeUpdate();
        }
    }

    /**
     * Từ chối hóa đơn (Hủy bỏ) và giải phóng lại các ca sân về trạng thái 'CÒN TRỐNG'.
     */
    private void rejectInvoice(String invoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE HoaDon SET trang_thai = 'ĐÃ HỦY' WHERE ma_hd = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, invoiceId);
                    stmt.executeUpdate();
                }

                // Giải phóng các sân liên quan về trạng thái trống
                String updateScheduleSql = "UPDATE LichSan SET trang_thai = 'CÒN TRỐNG' WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd = ?)";
                try (PreparedStatement scheduleStmt = conn.prepareStatement(updateScheduleSql)) {
                    scheduleStmt.setString(1, invoiceId);
                    scheduleStmt.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Gửi phản hồi JSON thành công về client.
     */
    private void sendSuccessResponse(HttpServletResponse resp, String message) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", message);
        
        PrintWriter out = resp.getWriter();
        out.print(JsonUtil.stringify(response));
        out.flush();
    }

    /**
     * Gửi phản hồi JSON lỗi về client.
     */
    private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("success", "false");
        response.put("error", message);
        
        PrintWriter out = resp.getWriter();
        out.print(JsonUtil.stringify(response));
        out.flush();
    }
}
