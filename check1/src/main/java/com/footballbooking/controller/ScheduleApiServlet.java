package com.footballbooking.controller;
import com.footballbooking.service.BookingService;
import com.footballbooking.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
/**
 * API Servlet cung cấp các điểm cuối JSON để truy vấn và cập nhật lịch trình đặt sân.
 * Được thiết kế để phục vụ các yêu cầu không đồng bộ (AJAX).
 */
@WebServlet("/api/schedule")
public class ScheduleApiServlet extends HttpServlet {
    private final BookingService service = new BookingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            // Lấy thông tin toàn bộ lịch trình đặt sân cho một ngày cụ thể
            String dateStr = req.getParameter("date");
            if (dateStr == null || dateStr.isBlank()) { 
                writeJson(resp, 400, error("Thiếu tham số ngày (date)")); 
                return; 
            }
            LocalDate date = LocalDate.parse(dateStr);
            writeJson(resp, 200, success(service.fullSchedule(date)));
        } catch (Exception e) { 
            writeJson(resp, 500, error(e.getMessage())); 
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String op = req.getParameter("op");
            // Kiểm tra hành động cập nhật hóa đơn hiện có
            if ("update".equals(op)) {
                handleUpdate(req, resp);
                return;
            }

            // Xử lý tạo mới một đơn đặt sân
            String[] fields = req.getParameterValues("fields");
            String dateStr = req.getParameter("date");
            String[] slotsStr = req.getParameterValues("slots");
            String name = req.getParameter("name");
            String phone = req.getParameter("phone");
            String mode = req.getParameter("mode"); // 'hold' - giữ chỗ hoặc 'direct' - đặt trực tiếp
            if (mode == null) mode = "hold";

            int[] cas = null;
            if (slotsStr != null) {
                cas = new int[slotsStr.length];
                for (int i = 0; i < slotsStr.length; i++) cas[i] = Integer.parseInt(slotsStr[i]);
            }

            // Thu thập các dịch vụ đi kèm được chọn từ form
            Map<String, Integer> services = new LinkedHashMap<>();
            java.util.Enumeration<String> params = req.getParameterNames();
            while (params.hasMoreElements()) {
                String p = params.nextElement();
                if (p.startsWith("service_")) {
                    String serviceId = p.substring(8);
                    String qtyParam = "qty_" + serviceId;
                    String qtyStr = req.getParameter(qtyParam);
                    int qty = qtyStr != null ? Integer.parseInt(qtyStr) : 1;
                    if (qty > 0) services.put(serviceId, qty);
                }
            }

            // Gọi service thực hiện lưu đơn đặt sân vào CSDL
            String invoiceId = service.book(fields, LocalDate.parse(dateStr), cas, name, phone, mode, services);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("invoiceId", invoiceId);
            data.put("redirectUrl", req.getContextPath() + "/invoice?id=" + invoiceId);
            writeJson(resp, 200, success(data));
        } catch (Exception e) { 
            writeJson(resp, 400, error(e.getMessage())); 
        }
    }

    /**
     * Xử lý yêu cầu cập nhật thông tin của một hóa đơn đang tồn tại.
     */
    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String invoiceId = req.getParameter("invoiceId");
            String[] fields = req.getParameterValues("fields");
            String dateStr = req.getParameter("date");
            String[] slotsStr = req.getParameterValues("slots");
            String name = req.getParameter("name");
            String phone = req.getParameter("phone");

            int[] cas = null;
            if (slotsStr != null) {
                cas = new int[slotsStr.length];
                for (int i = 0; i < slotsStr.length; i++) cas[i] = Integer.parseInt(slotsStr[i]);
            }

            // Thu thập danh sách dịch vụ cập nhật
            Map<String, Integer> services = new LinkedHashMap<>();
            java.util.Enumeration<String> params = req.getParameterNames();
            while (params.hasMoreElements()) {
                String p = params.nextElement();
                if (p.startsWith("service_")) {
                    String serviceId = p.substring(8);
                    String qtyParam = "qty_" + serviceId;
                    String qtyStr = req.getParameter(qtyParam);
                    int qty = qtyStr != null ? Integer.parseInt(qtyStr) : 1;
                    if (qty > 0) services.put(serviceId, qty);
                }
            }

            service.updateInvoice(invoiceId, fields, LocalDate.parse(dateStr), cas, name, phone, services);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("invoiceId", invoiceId);
            data.put("redirectUrl", req.getContextPath() + "/invoice?id=" + invoiceId);
            writeJson(resp, 200, success(data));
        } catch (Exception e) { 
            writeJson(resp, 400, error(e.getMessage())); 
        }
    }

    /**
     * Tạo đối tượng phản hồi JSON cho trường hợp thành công.
     */
    private Map<String, Object> success(Object data) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", true); m.put("data", data); return m;
    }

    /**
     * Tạo đối tượng phản hồi JSON cho trường hợp thất bại.
     */
    private Map<String, Object> error(String message) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", false); m.put("message", message); return m;
    }

    /**
     * Ghi dữ liệu JSON vào phản hồi HTTP.
     */
    private void writeJson(HttpServletResponse resp, int status, Map<String, Object> body) throws IOException {
        resp.setStatus(status); resp.getWriter().write(JsonUtil.stringify(body));
    }
}
