package com.footballbooking.controller;
import com.footballbooking.service.BookingService;
import com.footballbooking.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
@WebServlet("/api/schedule")
public class ScheduleApiServlet extends HttpServlet {
    private final BookingService service = new BookingService();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String dateStr = req.getParameter("date");
            if (dateStr == null || dateStr.isBlank()) { writeJson(resp, 400, error("Missing date")); return; }
            writeJson(resp, 200, success(service.fullSchedule(LocalDate.parse(dateStr))));
        } catch (Exception e) { writeJson(resp, 500, error(e.getMessage())); }
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String field = req.getParameter("field");
            String dateStr = req.getParameter("date");
            String slotStr = req.getParameter("slot");
            String name = req.getParameter("name");
            String phone = req.getParameter("phone");
            String invoiceId = service.book(field, LocalDate.parse(dateStr), Integer.parseInt(slotStr), name, phone);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("invoiceId", invoiceId);
            data.put("redirectUrl", req.getContextPath() + "/invoice?id=" + invoiceId);
            writeJson(resp, 200, success(data));
        } catch (Exception e) { writeJson(resp, 400, error(e.getMessage())); }
    }
    private Map<String, Object> success(Object data) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", true); m.put("data", data); return m;
    }
    private Map<String, Object> error(String message) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", false); m.put("message", message); return m;
    }
    private void writeJson(HttpServletResponse resp, int status, Map<String, Object> body) throws IOException {
        resp.setStatus(status); resp.getWriter().write(JsonUtil.stringify(body));
    }
}