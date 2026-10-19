package com.footballbooking.controller;
import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
@WebServlet("/booking")
public class BookingServlet extends HttpServlet {
    private final BookingService service = new BookingService();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        req.setAttribute("today", today);
        LocalDate viewDate = today;
        String dateParam = req.getParameter("date");
        if (dateParam != null && !dateParam.isBlank()) {
            try { viewDate = LocalDate.parse(dateParam); } catch (Exception ignored) { viewDate = today; }
        }
        req.setAttribute("viewDate", viewDate);
        try {
            Map<String, Object> data = service.fullSchedule(viewDate);
            req.setAttribute("fields", data.get("fields"));
            req.setAttribute("schedule", data.get("schedule"));
            req.setAttribute("headers", data.get("headers"));
            List<Map<String, Object>> services = new ArrayList<>();
            try {
                services = new com.footballbooking.repository.BookingRepository().services();
            } catch (Exception e) {}
            req.setAttribute("services", services);
        } catch (Exception e) {
            req.setAttribute("error", "Lỗi database: " + e.getMessage());
            try {
                Map<String, Object> data = service.emptySchedule(viewDate);
                req.setAttribute("fields", data.get("fields"));
                req.setAttribute("schedule", data.get("schedule"));
                req.setAttribute("headers", data.get("headers"));
            } catch (Exception ex) {
                req.setAttribute("fields", new java.util.ArrayList<>());
                java.util.Map<String, java.util.List> empty = new java.util.LinkedHashMap<>();
                req.setAttribute("schedule", empty);
                java.util.List<java.util.Map<String, String>> headers = new java.util.ArrayList<>();
                for (String[] s : com.footballbooking.utils.Slots.DATA) {
                    java.util.Map<String, String> h = new java.util.LinkedHashMap<>();
                    h.put("so_ca", s[1]); h.put("label", s[4]); headers.add(h);
                }
                req.setAttribute("headers", headers);
            }
            req.setAttribute("services", new ArrayList<>());
        }
        req.getRequestDispatcher("/jsp/user/booking.jsp").forward(req, resp);
    }
}