# 02_QUAN LY SAN_DAT SAN.md
## PHẦN 2 — NGƯỜI 2: QUẢN LÝ SÂN + ĐẶT SÂN

---

## 1. CHỨC NĂNG PHỤ TRÁCH

Người 2 phụ trách toàn bộ chức năng liên quan đến sân bóng và đặt sân:

- Quản lý sân (CRUD)
- Quản lý lịch sân
- Đặt sân (Booking)
- Chọn sân, chọn ngày, chọn ca
- Hủy sân
- Kiểm tra trạng thái sân
- Quản lý trạng thái: AVAILABLE, PENDING, BOOKED, EXPIRED

**Lưu ý quan trọng:**
- Không code chức năng đăng nhập, voucher, hóa đơn, thanh toán, hay CronJob
- Chỉ làm việc trên các file liên quan đến sân và đặt sân
- Nếu cần sửa file của người khác, phải đánh dấu "FILE DÙNG CHUNG"

---

## 2. CẤU TRÚC THƯ MỤC

```
src/main/java/com/footballbooking/
├── config/
│   ├── DBConnection.java                [CLASS DÙNG CHUNG]
│   └── ExpiryListener.java              [CLASS DÙNG CHUNG]
├── controller/
│   ├── BookingServlet.java              (Đặt sân)
│   ├── ScheduleApiServlet.java          (API lịch sân)
│   ├── HomeServlet.java                 (Trang chủ)
│   ├── AdminCrudServlet.java            [CLASS DÙNG CHUNG]
│   └── ... (các Servlet khác do người khác phụ trách)
├── model/
│   └── Entities.java                    [CLASS DÙNG CHUNG]
├── repository/
│   └── BookingRepository.java           (Repository đặt sân)
├── service/
│   └── BookingService.java              (Service đặt sân)
└── utils/
    ├── DbConnection.java                [CLASS DÙNG CHUNG]
    ├── JsonUtil.java                    [CLASS DÙNG CHUNG]
    └── Slots.java                       [CLASS DÙNG CHUNG]

src/main/webapp/jsp/
├── user/
│   ├── home.jsp                        (Trang chủ)
│   ├── booking.jsp                     (Đặt sân)
│   └── invoice.jsp                     [CLASS DÙNG CHUNG]
└── admin/
    └── field-dashboard.jsp             (Quản lý sân)

database/
└── football_booking.sql                [CLASS DÙNG CHUNG]
```

---

## 3. DATABASE LIÊN QUAN

### 3.1 Bảng San (Sân bóng)
```sql
CREATE TABLE San (
    ma_san      VARCHAR(10)  PRIMARY KEY,
    ten_san     NVARCHAR(100) NOT NULL,
    loai_san    NVARCHAR(30)  NOT NULL,
    gia_theo_gio DECIMAL(18,2) NOT NULL CHECK (gia_theo_gio >= 0),
    trang_thai  NVARCHAR(20)  NOT NULL DEFAULT N'HOẠT ĐỘNG'
);
```

**Dữ liệu mẫu:**
```sql
INSERT INTO San (ma_san, ten_san, loai_san, gia_theo_gio, trang_thai) VALUES
('S01', N'Sân 1', N'Sân 7 người', 600000, N'HOẠT ĐỘNG'),
('S02', N'Sân 2', N'Sân 7 người', 600000, N'HOẠT ĐỘNG'),
('S03', N'Sân 3', N'Sân 7 người', 600000, N'HOẠT ĐỘNG'),
('S04', N'Sân 4', N'Sân 7 người', 600000, N'HOẠT ĐỘNG');
```

### 3.2 Bảng LichSan (Lịch sân)
```sql
CREATE TABLE LichSan (
    ma_lich      VARCHAR(10) PRIMARY KEY,
    ma_san       VARCHAR(10) NOT NULL REFERENCES San(ma_san),
    ngay_dat     DATE NOT NULL,
    buoi         NVARCHAR(20) NOT NULL,
    ca           NVARCHAR(30) NOT NULL,
    so_ca        TINYINT NOT NULL CHECK (so_ca BETWEEN 1 AND 9),
    gio_bat_dau  DATETIME NOT NULL,
    gio_ket_thuc DATETIME NOT NULL,
    trang_thai   NVARCHAR(20) NOT NULL DEFAULT N'CÒN TRỐNG',
    CONSTRAINT UQ_LichSan UNIQUE (ma_san, ngay_dat, so_ca)
);
```

**Trạng thái lịch sân:**
- `CÒN TRỐNG` - Sân còn trống, có thể đặt
- `ĐANG GIỮ CHỖ` - Đang giữ chỗ (chờ thanh toán)
- `ĐÃ ĐẶT` - Đã đặt và thanh toán
- `SỰ KIỆN` - Được dùng cho sự kiện
- `HẾT` - Ca đã qua giờ bắt đầu

### 3.3 Bảng KhachHang (Khách hàng)
```sql
CREATE TABLE KhachHang (
    ma_kh   VARCHAR(10) PRIMARY KEY,
    ho_ten  NVARCHAR(100) NOT NULL,
    sdt     VARCHAR(15) NOT NULL UNIQUE,
    email   VARCHAR(100) NULL
);
```

**Lưu ý:** Khách hàng được tạo tự động khi đặt sân.

### 3.4 Bảng CTHD (Chi tiết hóa đơn) - liên quan
```sql
CREATE TABLE CTHD (
    ma_cthd    INT IDENTITY PRIMARY KEY,
    ma_hd      VARCHAR(10) NOT NULL REFERENCES HoaDon(ma_hd),
    ma_lich    VARCHAR(10) NOT NULL REFERENCES LichSan(ma_lich),
    ma_dv      VARCHAR(10) NULL REFERENCES DichVu(ma_dv),
    so_luong   INT NOT NULL DEFAULT 1,
    don_gia    DECIMAL(18,2) NOT NULL,
    thanh_tien DECIMAL(18,2) NOT NULL
);
```

---

## 4. CÁC ENTITY

### 4.1 Entity San
```java
public static class San {
    public String maSan, tenSan, loaiSan, trangThai;
    public double giaTheoGio;
}
```

**Giải thích:**
- `maSan`: Mã sân (VD: S01)
- `tenSan`: Tên sân
- `loaiSan`: Loại sân (Sân 5 người, Sân 7 người...)
- `giaTheoGio`: Giá thuê theo giờ
- `trangThai`: Trạng thái (HOẠT ĐỘNG, BẢO TRÌ, ĐÃ XÓA)

### 4.2 Entity LichSan
```java
public static class LichSan {
    public String maLich, maSan, buoi, ca, gioBatDau, gioKetThuc, trangThai;
    public LocalDate ngayDat;
    public int soCa;
}
```

**Giải thích:**
- `maLich`: Mã lịch (VD: L001)
- `maSan`: Mã sân
- `buoi`: Buổi (Sáng, Chiều, Tối)
- `ca`: Ca (VD: 06:00 - 08:00)
- `so_ca`: Số thứ tự ca (1-9)
- `gio_bat_dau`, `gio_ket_thuc`: Giờ bắt đầu và kết thúc
- `ngay_dat`: Ngày đặt
- `trangThai`: Trạng thái (CÒN TRỐNG, ĐANG GIỮ CHỖ, ĐÃ ĐẶT...)

### 4.3 Entity KhachHang
```java
public static class KhachHang {
    public String maKh, hoTen, sdt, email;
}
```

**Giải thích:**
- `maKh`: Mã khách hàng (VD: KH001)
- `hoTen`: Họ tên
- `sdt`: Số điện thoại
- `email`: Email (tùy chọn)

---

## 5. CÁC SERVLET

### 5.1 BookingServlet.java
**Đường dẫn:** `/booking`
**Chức năng:** Xử lý đặt sân của người dùng

**Code hoàn chỉnh:**
```java
package com.footballbooking.controller;

import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Servlet xử lý quy trình đặt sân của người dùng.
 * Quản lý việc chọn ca sân, thêm dịch vụ và xác nhận đơn đặt sân thông qua Session.
 */
@WebServlet("/booking")
public class BookingServlet extends HttpServlet {

    private final BookingService service = new BookingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        LocalDate today = LocalDate.now();
        req.setAttribute("today", today);

        // Xóa các hóa đơn trống để làm sạch CSDL
        try { service.deleteEmptyInvoices(); } catch (Exception ignored) {}

        // Xác định ngày người dùng muốn xem lịch đặt sân
        String dateParam = req.getParameter("date");
        LocalDate viewDate = today;
        if (dateParam != null && !dateParam.isBlank()) {
            try { viewDate = LocalDate.parse(dateParam); }
            catch (Exception ignored) { }
        }
        req.setAttribute("viewDate", viewDate);

        // Lấy thông tin hóa đơn đang thao tác từ Session
        String hdId = (String) session.getAttribute("activeInvoiceId");
        String editId = req.getParameter("edit");
        if (editId != null && !editId.isBlank()) {
            hdId = editId;
            session.setAttribute("activeInvoiceId", hdId);
        }

        // Nếu đang có hóa đơn hoạt động, tải thông tin chi tiết để hiển thị
        if (hdId != null) {
            try {
                Map<String, Object> inv = service.invoice(hdId);
                if (inv != null) {
                    req.setAttribute("invoice", inv);
                    req.setAttribute("editName", inv.get("ho_ten"));
                    req.setAttribute("editPhone", inv.get("sdt"));
                    req.setAttribute("activeInvoiceId", hdId);
                    req.setAttribute("vouchers", service.getAvailableVouchers(hdId));
                } else {
                    // Nếu hóa đơn không tồn tại (đã bị xóa hoặc sai ID), dọn dẹp session
                    session.removeAttribute("activeInvoiceId");
                    hdId = null;
                }
            } catch (Exception e) {
                req.setAttribute("error", "Lỗi tải hóa đơn: " + e.getMessage());
            }
        }

        // Tải dữ liệu lịch sân và dịch vụ để hiển thị lên giao diện đặt sân
        try {
            Map<String, Object> data = service.fullSchedule(viewDate);
            req.setAttribute("fields", data.get("fields"));
            req.setAttribute("schedule", data.get("schedule"));
            req.setAttribute("headers", data.get("headers"));
            req.setAttribute("services", service.services());
        } catch (Exception e) {
            req.setAttribute("error", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        }
        req.getRequestDispatcher("/jsp/user/booking.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String op = req.getParameter("op");
        String dateStr = req.getParameter("date");
        String hdId = (String) session.getAttribute("activeInvoiceId");

        try {
            hdId = handleOperation(op, req, session, hdId);
            if (hdId != null && !"confirm".equals(op)) {
                session.setAttribute("activeInvoiceId", hdId);
            }
            if ("confirm".equals(op)) {
                resp.sendRedirect(req.getContextPath() + "/invoice?id=" + hdId);
                return;
            }
        } catch (Exception e) {
            session.setAttribute("bookingError", e.getMessage());
        }

        String redirectUrl = req.getContextPath() + "/booking" + (dateStr != null ? "?date=" + dateStr : "");
        resp.sendRedirect(redirectUrl);
    }

    private String handleOperation(String op, HttpServletRequest req, HttpSession session, String hdId) throws Exception {
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");

        if ("selectSlot".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone);
            String fieldId = req.getParameter("fieldId");
            int slot = Integer.parseInt(req.getParameter("slot"));
            LocalDate date = LocalDate.parse(req.getParameter("date"));
            service.toggleSlot(hdId, fieldId, date, slot);
        } else if ("addService".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone);
            String svId = req.getParameter("serviceId");
            int qty = Integer.parseInt(req.getParameter("qty"));
            service.updateService(hdId, svId, qty);
        } else if ("removeService".equals(op)) {
            if (hdId != null) {
                service.updateService(hdId, req.getParameter("serviceId"), 0);
            }
        } else if ("applyVoucher".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone);
            String vId = req.getParameter("voucherId");
            service.applyVoucher(hdId, vId);
        } else if ("confirm".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone);
            session.removeAttribute("activeInvoiceId");
        }
        return hdId;
    }
}
```

**Giải thích luồng xử lý:**
1. Người dùng truy cập `/booking`
2. `doGet()` tải danh sách sân, lịch sân, dịch vụ
3. Người dùng chọn sân/ca → gửi POST với `op=selectSlot`
4. Người dùng thêm dịch vụ → gửi POST với `op=addService`
5. Người dùng xác nhận → gửi POST với `op=confirm`
6. Chuyển đến trang `/invoice` để thanh toán

### 5.2 ScheduleApiServlet.java
**Đường dẫn:** `/api/schedule`
**Chức năng:** API JSON để truy vấn và cập nhật lịch sân

**Code hoàn chỉnh:**
```java
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
```

### 5.3 HomeServlet.java
**Đường dẫn:** `/home`
**Chức năng:** Hiển thị trang chủ với danh sách sân và thống kê

**Code hoàn chỉnh:**
```java
package com.footballbooking.controller;

import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

/**
 * Servlet hiển thị trang chủ của người dùng.
 * Liệt kê danh sách các sân bóng hiện có và thống kê trạng thái các ca trong ngày.
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        BookingService sv = new BookingService();
        try {
            // Lấy danh sách các sân và thống kê đặt sân trong ngày hôm nay
            q.setAttribute("fields", sv.fields());
            q.setAttribute("stats", sv.fieldStats());
        } catch (Exception e) {
            // Xử lý fallback khi có lỗi kết nối CSDL hoặc dữ liệu trống
            List<Map<String, Object>> f = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("ma_san", "S0" + i);
                r.put("ten_san", "Sân " + i);
                r.put("loai_san", "Sân 7 người");
                r.put("gia_theo_gio", 400000 + i * 50000);
                f.add(r);
            }
            q.setAttribute("fields", f);
            q.setAttribute("stats", Map.of("booked", 0, "pending", 0, "expired", 0, "free", 0));
        }
        // Chuyển tiếp đến trang giao diện người dùng
        q.getRequestDispatcher("/jsp/user/home.jsp").forward(q, p);
    }
}
```

---

## 6. CÁC SERVICE

### 6.1 BookingService.java
**Đường dẫn:** `src/main/java/com/footballbooking/service/BookingService.java`
**Chức năng:** Tầng Service xử lý nghiệp vụ đặt sân

**Code hoàn chỉnh:**
```java
package com.footballbooking.service;

import com.footballbooking.repository.BookingRepository;
import com.footballbooking.utils.Slots;
import java.time.LocalDate;
import java.util.*;


/**
 * Lớp cung cấp các dịch vụ nghiệp vụ liên quan đến đặt sân.
 * Đóng vai trò là tầng trung gian (Service Layer) giữa Controller và Repository.
 */
public class BookingService {
    private final BookingRepository dao = new BookingRepository();
    public List<Map<String, Object>> fields() throws Exception { return dao.fields(); }
    public List<Map<String, Object>> services() throws Exception { return dao.services(); }
    /**
     * Lấy toàn bộ lịch trình đặt sân của tất cả các sân trong một ngày.
     */
    public Map<String, Object> fullSchedule(LocalDate date) throws Exception {
        try {
            List<Map<String, Object>> fields = dao.fields();
            Map<String, List<Map<String, Object>>> schedule = new LinkedHashMap<>();
            for (Map<String, Object> f : fields) {
                String maSan = String.valueOf(f.get("ma_san"));
                schedule.put(maSan, dao.slots(maSan, date));
            }
            return buildScheduleResponse(date, fields, schedule);
        } catch (Exception e) {
            return emptySchedule(date);
        }
    }

    private Map<String, Object> buildScheduleResponse(LocalDate date, List<Map<String, Object>> fields, Map<String, List<Map<String, Object>>> schedule) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("date", date.toString());
        r.put("fields", fields);
        r.put("schedule", schedule);
        r.put("headers", makeHeaders());
        return r;
    }

    /**
     * Tạo dữ liệu lịch trình trống khi không thể kết nối cơ sở dữ liệu.
     */
    public Map<String, Object> emptySchedule(LocalDate date) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            fields.add(createDefaultField(i));
        }
        Map<String, List<Map<String, Object>>> schedule = new LinkedHashMap<>();
        for (Map<String, Object> x : fields) {
            schedule.put(String.valueOf(x.get("ma_san")), freeSlots());
        }
        return buildScheduleResponse(date, fields, schedule);
    }

    private Map<String, Object> createDefaultField(int i) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ma_san", "S0" + i);
        r.put("ten_san", "Sân " + i);
        r.put("loai_san", "Sân 7 người");
        r.put("gia_theo_gio", (400000 + i * 50000) * 2);
        r.put("trang_thai", "HOẠT ĐỘNG");
        return r;
    }
    private List<Map<String, String>> makeHeaders() {
        List<Map<String, String>> h = new ArrayList<>();
        for (String[] s : Slots.DATA) h.add(Map.of("so_ca", s[1], "label", s[4]));
        return h;
    }
    private List<Map<String, Object>> freeSlots() {
        List<Map<String, Object>> l = new ArrayList<>();
        for (String[] s : Slots.DATA) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("so_ca", Integer.parseInt(s[1])); r.put("buoi", s[0]);
            r.put("gio_bat_dau", s[2]); r.put("gio_ket_thuc", s[3]);
            r.put("nhan_ca", s[4]); r.put("status_code", 0);
            l.add(r);
        }
        return l;
    }
    /**
     * Xử lý nghiệp vụ đặt sân: Kiểm tra tính hợp lệ của dữ liệu trước khi lưu.
     */
    public String book(String[] fields, LocalDate date, int[] cas, String name, String phone, String mode, Map<String, Integer> services) throws Exception {
        // Kiểm tra ngày đặt không được ở quá khứ
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Không thể đặt ngày trong quá khứ");
        // Kiểm tra thông tin khách hàng
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Họ tên trống");
        // Kiểm tra định dạng số điện thoại Việt Nam
        if (phone == null || !phone.matches("^(0|\\+84)[0-9]{9}$")) throw new IllegalArgumentException("Số điện thoại sai");
        // Kiểm tra xem đã chọn sân chưa
        if (fields == null || fields.length == 0) throw new IllegalArgumentException("Chưa chọn sân");
        if (mode == null) mode = "hold";
        return dao.createBooking(fields, date, cas, name.trim(), phone.trim(), mode, services);
    }
    public void updateInvoice(String invoiceId, String[] fields, LocalDate date, int[] cas, String name, String phone, Map<String, Integer> services) throws Exception {
        dao.updateInvoice(invoiceId, fields, date, cas, name, phone, services);
    }
    public Map<String, Integer> fieldStats() throws Exception { return dao.fieldStats(); }
    public void applyVoucher(String hdId, String voucherId) throws Exception {
        dao.applyVoucher(hdId, voucherId);
    }
    public List<Map<String, Object>> getAvailableVouchers(String hdId) throws Exception {
        return dao.getAvailableVouchers(hdId);
    }
    public Map<String, Object> invoice(String id) throws Exception { return dao.invoice(id); }
    public boolean pay(String id) throws Exception { return dao.pay(id); }

    public void payDeposit(String id) throws Exception {
        dao.pay(id);
    }

    public void payRemaining(String id) throws Exception {
        dao.payRemaining(id);
    }

    public void cancelInvoice(String id) throws Exception {
        dao.cancelInvoice(id);
    }

    public void deleteEmptyInvoices() throws Exception {
        dao.deleteEmptyInvoices();
    }

    public Map<String, Object> invoiceForEdit(String id) throws Exception {
        return dao.invoiceForEdit(id);
    }

    public String ensureInvoice(String hdId, String name, String phone) throws Exception {
        return dao.ensureInvoice(hdId, name, phone);
    }
    public void toggleSlot(String hdId, String fieldId, LocalDate date, int ca) throws Exception {
        dao.toggleSlot(hdId, fieldId, date, ca);
    }
    public void updateService(String hdId, String serviceId, int qty) throws Exception {
        dao.updateService(hdId, serviceId, qty);
    }
}
```

**Giải thích các method quan trọng:**

### 6.1.1 book()
```java
public String book(String[] fields, LocalDate date, int[] cas, String name, String phone, String mode, Map<String, Integer> services)
```
**Mục đích:** Tạo đơn đặt sân mới
**Luồng:**
1. Kiểm tra tính hợp lệ (ngày không quá khứ, thông tin KH hợp lệ)
2. Gọi `dao.createBooking()` để tạo hóa đơn và chi tiết

### 6.1.2 ensureInvoice()
```java
public String ensureInvoice(String hdId, String name, String phone)
```
**Mục đích:** Đảm bảo luôn có hóa đơn hoạt động để lưu lựa chọn
**Luồng:**
1. Nếu có hdId và tồn tại → cập nhật thông tin KH
2. Nếu không → tạo hóa đơn mới

### 6.1.3 toggleSlot()
```java
public void toggleSlot(String hdId, String fieldId, LocalDate date, int ca)
```
**Mục đích:** Bật/tắt chọn sân trong hóa đơn
**Luồng:**
1. Tìm lịch sân đã chọn
2. Nếu có → xóa (bỏ chọn)
3. Nếu không → thêm mới (chọn)

---

## 7. CÁC REPOSITORY

### 7.1 BookingRepository.java
**Đường dẫn:** `src/main/java/com/footballbooking/repository/BookingRepository.java`
**Chức năng:** Repository quản lý đặt sân và hóa đơn

**Code hoàn chỉnh (1189 dòng):**
```java
package com.footballbooking.repository;

import com.footballbooking.config.DBConnection;
import com.footballbooking.utils.Slots;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lớp quản lý truy xuất và thao tác dữ liệu liên quan đến đặt sân và hóa đơn.
 * Chứa toàn bộ các câu lệnh SQL và logic nghiệp vụ tương tác với cơ sở dữ liệu.
 */
public class BookingRepository {

    // Định nghĩa các hằng số trạng thái của sân và hóa đơn
    private static final String FREE = "CÒN TRỐNG";
    private static final String HOLD = "ĐANG GIỮ CHỖ";
    private static final String BOOKED = "ĐÃ ĐẶT";
    private static final String EXPIRED = "HẾT";
    private static final String UNPAID = "CHƯA THANH TOÁN";
    private static final String PROCESSING = "ĐANG XỬ LÝ";
    private static final String DEPOSITED = "MỚI THANH TOÁN CỌC";
    private static final String PAID = "ĐÃ THANH TOÁN";
    private static final String CANCEL = "ĐÃ HỦY";
    private static final String EVENT = "SỰ KIỆN";

    static {
        // Thực hiện nâng cấp cấu trúc bảng (Migration) nếu thiếu các cột cần thiết
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            // Thêm các cột mới vào bảng HoaDon nếu chưa tồn tại
            s.execute("IF COL_LENGTH('HoaDon','han_thanh_toan') IS NULL ALTER TABLE HoaDon ADD han_thanh_toan DATETIME NULL");
            s.execute("IF COL_LENGTH('HoaDon','tien_coc') IS NULL ALTER TABLE HoaDon ADD tien_coc DECIMAL(18,2) NOT NULL DEFAULT 200000");
            s.execute("IF COL_LENGTH('HoaDon','so_tien_con_lai') IS NULL ALTER TABLE HoaDon ADD so_tien_con_lai DECIMAL(18,2) NOT NULL DEFAULT 0");
            s.execute("IF COL_LENGTH('HoaDon','ma_voucher') IS NULL ALTER TABLE HoaDon ADD ma_voucher VARCHAR(20) NULL");
            s.execute("IF COL_LENGTH('HoaDon','tien_giam') IS NULL ALTER TABLE HoaDon ADD tien_giam DECIMAL(18,2) NOT NULL DEFAULT 0");

            // Bảng Voucher theo cấu trúc: ma_voucher, ma_code, ten_voucher, loai_giam, gia_tri,
            // don_toi_thieu, ngay_bat_dau, ngay_ket_thuc, so_luong, trang_thai, loai
            // loai: 1=Hóa đơn, 2=Sân, 3=Dịch vụ
            s.execute("IF OBJECT_ID('Voucher', 'U') IS NULL CREATE TABLE Voucher (" +
                    "ma_voucher VARCHAR(20) PRIMARY KEY, " +
                    "ma_code VARCHAR(50) NOT NULL, " +
                    "ten_voucher NVARCHAR(100) NOT NULL, " +
                    "loai_giam VARCHAR(20) NOT NULL, " +
                    "gia_tri FLOAT NOT NULL DEFAULT 0, " +
                    "don_toi_thieu FLOAT NOT NULL DEFAULT 0, " +
                    "ngay_bat_dau DATE NOT NULL, " +
                    "ngay_ket_thuc DATE NOT NULL, " +
                    "so_luong INT NOT NULL DEFAULT 0, " +
                    "trang_thai NVARCHAR(30) NOT NULL DEFAULT N'HOẠT ĐỘNG', " +
                    "loai INT NOT NULL DEFAULT 1)");
            s.execute("IF COL_LENGTH('Voucher','loai') IS NULL ALTER TABLE Voucher ADD loai INT NOT NULL DEFAULT 1");
            s.execute("IF COL_LENGTH('Voucher','ma_code') IS NULL ALTER TABLE Voucher ADD ma_code VARCHAR(50) NULL");
            s.execute("IF COL_LENGTH('Voucher','ten_voucher') IS NULL ALTER TABLE Voucher ADD ten_voucher NVARCHAR(100) NULL");
            s.execute("IF COL_LENGTH('Voucher','loai_giam') IS NULL ALTER TABLE Voucher ADD loai_giam VARCHAR(20) NULL");
            s.execute("IF COL_LENGTH('Voucher','gia_tri') IS NULL ALTER TABLE Voucher ADD gia_tri FLOAT NULL");
            s.execute("IF COL_LENGTH('Voucher','don_toi_thieu') IS NULL ALTER TABLE Voucher ADD don_toi_thieu FLOAT NULL");
            s.execute("IF COL_LENGTH('Voucher','ngay_bat_dau') IS NULL ALTER TABLE Voucher ADD ngay_bat_dau DATE NULL");
            s.execute("IF COL_LENGTH('Voucher','ngay_ket_thuc') IS NULL ALTER TABLE Voucher ADD ngay_ket_thuc DATE NULL");
            s.execute("IF COL_LENGTH('Voucher','so_luong') IS NULL ALTER TABLE Voucher ADD so_luong INT NULL");
            s.execute("IF COL_LENGTH('Voucher','trang_thai') IS NULL ALTER TABLE Voucher ADD trang_thai NVARCHAR(30) NULL");
            // Giữ cột legacy (nếu còn) để không phá dữ liệu cũ; map runtime qua VoucherRepository

            s.execute("IF OBJECT_ID('Voucher_San', 'U') IS NULL CREATE TABLE Voucher_San (" +
                    "ma_voucher VARCHAR(20), " +
                    "ma_san VARCHAR(10) REFERENCES San(ma_san), " +
                    "PRIMARY KEY (ma_voucher, ma_san))");

            s.execute("IF OBJECT_ID('Voucher_DichVu', 'U') IS NULL CREATE TABLE Voucher_DichVu (" +
                    "ma_voucher VARCHAR(20), " +
                    "ma_dv VARCHAR(10) REFERENCES DichVu(ma_dv), " +
                    "PRIMARY KEY (ma_voucher, ma_dv))");
            
            // Thêm các cột bổ sung cho bảng LichSan để quản lý ca sân tốt hơn
            s.execute("IF COL_LENGTH('LichSan','so_ca') IS NULL ALTER TABLE LichSan ADD so_ca TINYINT NULL");
            s.execute("IF COL_LENGTH('LichSan','ngay_dat') IS NULL ALTER TABLE LichSan ADD ngay_dat DATE NULL");
            s.execute("IF COL_LENGTH('LichSan','buoi') IS NULL ALTER TABLE LichSan ADD buoi NVARCHAR(20) NULL");
            
            // Đảm bảo bảng NhanVien có cột mật khẩu để đăng nhập admin
            s.execute("IF COL_LENGTH('NhanVien','mat_khau') IS NULL ALTER TABLE NhanVien ADD mat_khau NVARCHAR(100) NULL");

            // Khởi tạo dữ liệu mặc định cho Admin và chuẩn hóa dữ liệu cũ
            s.execute("UPDATE NhanVien SET mat_khau=N'admin123' WHERE mat_khau IS NULL OR mat_khau=''");
            s.execute("IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE ma_nv='NV001' OR sdt='0900000000') " +
                      "INSERT INTO NhanVien (ma_nv, ten_nv, sdt, chuc_vu, mat_khau) VALUES ('NV001', N'Quản trị viên', '0900000000', N'Admin', N'admin123')");
            s.execute("UPDATE LichSan SET ngay_dat=CAST(gio_bat_dau AS DATE) WHERE ngay_dat IS NULL AND gio_bat_dau IS NOT NULL");
            s.execute("UPDATE LichSan SET buoi=ca WHERE buoi IS NULL AND ca IS NOT NULL");
        } catch (Exception e) {
            System.err.println("Lỗi Migration Schema: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách các sân đang hoạt động.
     * @return List chứa thông tin các sân
     */
    public List<Map<String, Object>> fields() throws SQLException {
        return query("SELECT ma_san,ten_san,loai_san,gia_theo_gio,trang_thai FROM San WHERE trang_thai=N'HOẠT ĐỘNG' "
                + "ORDER BY CASE ma_san WHEN 'S01' THEN 1 WHEN 'S02' THEN 2 WHEN 'SVIP' THEN 3 "
                + "WHEN 'S04' THEN 4 WHEN 'S05' THEN 5 WHEN 'S06' THEN 6 WHEN 'S07' THEN 7 WHEN 'S08' THEN 8 ELSE 99 END");
    }

    /**
     * Lấy danh sách tất cả các dịch vụ.
     */
    public List<Map<String, Object>> services() throws SQLException {
        return query("SELECT ma_dv,ten_dv,don_gia FROM DichVu ORDER BY ma_dv");
    }

    /**
     * Lấy trạng thái các ca (slots) của một sân cụ thể trong một ngày nhất định.
     * @param san Mã sân
     * @param d Ngày cần xem lịch
     * @return Danh sách các ca kèm trạng thái (Trống, Đã đặt, Chờ...)
     */
    public List<Map<String, Object>> slots(String san, LocalDate d) throws SQLException {
        StringBuilder values = new StringBuilder();
        // Xây dựng bảng tạm chứa 9 ca trong ngày từ Slots.DATA
        for (String[] x : Slots.DATA) {
            if (values.length() > 0) values.append(',');
            values.append('(').append(x[1]).append(",N'").append(x[0]).append("','")
                    .append(x[2]).append("','").append(x[3]).append("','").append(x[4]).append("')");
        }
        // Sử dụng GROUP BY và MAX(status_code) để tránh lặp lại ca khi có nhiều bản ghi trạng thái
        String sql = "SELECT x.so_ca, x.buoi, x.gio_bat_dau, x.gio_ket_thuc, x.nhan_ca, "
                + "MAX(CASE WHEN l.trang_thai=N'" + BOOKED + "' THEN 2 WHEN l.trang_thai=N'" + EVENT + "' THEN 4 "
                + "WHEN l.trang_thai=N'" + HOLD + "' THEN 1 WHEN l.trang_thai=N'" + EXPIRED + "' THEN 3 ELSE 0 END) as status_code "
                + "FROM (VALUES " + values + ") x(so_ca,buoi,gio_bat_dau,gio_ket_thuc,nhan_ca) "
                + "LEFT JOIN LichSan l ON l.ma_san=? AND l.ngay_dat=? AND l.so_ca=x.so_ca "
                + "GROUP BY x.so_ca, x.buoi, x.gio_bat_dau, x.gio_ket_thuc, x.nhan_ca "
                + "ORDER BY x.so_ca";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, san);
            p.setDate(2, Date.valueOf(d));
            List<Map<String, Object>> list = rows(p.executeQuery());
            for (Map<String, Object> row : list) {
                int sc = ((Number) row.get("status_code")).intValue();
                // Nếu ca chưa có bản ghi trong LichSan (status_code=0) nhưng giờ bắt đầu
                // đã qua thời điểm hiện tại trong ngày hôm nay → coi như "HẾT"
                if (sc == 0 && d.equals(LocalDate.now())) {
                    String startTime = String.valueOf(row.get("gio_bat_dau"));
                    try {
                        LocalTime start = LocalTime.parse(startTime);
                        if (!start.isAfter(LocalTime.now())) {
                            sc = 3;
                            row.put("status_code", sc);
                        }
                    } catch (Exception ignored) { }
                }
                // Map lại trạng thái chuỗi dựa trên mã trạng thái cao nhất tìm thấy
                row.put("trang_thai", sc == 2 ? BOOKED : sc == 4 ? EVENT : sc == 1 ? HOLD : sc == 3 ? EXPIRED : FREE);
            }
            return list;
        }
    }

    /**
     * Tạo một đơn đặt sân mới.
     */
    public String createBooking(String[] fields, LocalDate day, int[] cas, String name, String phone, String mode, Map<String, Integer> services) throws Exception {
        validateBookingData(fields, cas);
        checkSlotsAvailability(fields, day, cas);

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String kh = customer(c, name, phone);
                String hd = next(c, "HoaDon", "ma_hd", "HD", 6);
                List<String> lichIds = new ArrayList<>();
                List<Double> slotPrices = new ArrayList<>();

                double baseTotal = processBookingSlots(c, fields, day, cas, mode, null, lichIds, slotPrices);
                double serviceTotal = calculateServiceTotal(c, services);
                double total = baseTotal + serviceTotal;
                double deposit = "direct".equals(mode) ? total : (total * 0.3);
                String status = "direct".equals(mode) ? PAID : UNPAID;

                insertHoaDon(c, hd, kh, total, deposit, status);
                insertCthdForSlots(c, hd, lichIds, slotPrices);
                insertCthdForServices(c, hd, lichIds.get(0), services);

                c.commit();
                return hd;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void validateBookingData(String[] fields, int[] cas) {
        if (fields == null || cas == null || fields.length != cas.length || fields.length == 0)
            throw new IllegalArgumentException("Dữ liệu đặt sân không hợp lệ");
    }

    private void checkSlotsAvailability(String[] fields, LocalDate day, int[] cas) throws SQLException {
        for (int i = 0; i < fields.length; i++) {
            if (!isSlotFree(fields[i], day, cas[i]))
                throw new IllegalStateException("Sân " + fields[i] + " ca " + cas[i] + " đã được đặt hoặc đang giữ chỗ");
        }
    }

    private double processBookingSlots(Connection c, String[] fields, LocalDate day, int[] cas, String mode, String status, List<String> lichIds, List<Double> slotPrices) throws SQLException {
        double baseTotal = 0;
        for (int i = 0; i < fields.length; i++) {
            String san = fields[i];
            int ca = cas[i];
            String lich = next(c, "LichSan", "ma_lich", "L", 3);
            lichIds.add(lich);

            insertLichSanRecord(c, lich, san, ca, day, mode, status);

            double price = getFieldPrice(c, san);
            baseTotal += price;
            slotPrices.add(price);
        }
        return baseTotal;
    }

    private void insertLichSanRecord(Connection c, String lich, String san, int ca, LocalDate day, String mode, String status) throws SQLException {
        String[] sl = Slots.get(ca);
        LocalTime start = LocalTime.parse(sl[2]);
        LocalTime end = LocalTime.parse(sl[3]);
        LocalDateTime startDt = day.atTime(start);
        LocalDateTime endDt = day.atTime(end);
        if (!end.isAfter(start)) endDt = endDt.plusDays(1);

        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO LichSan(ma_lich,ma_san,ca,gio_bat_dau,gio_ket_thuc,trang_thai,ngay_dat,buoi,so_ca) VALUES(?,?,?,?,?,?,?,?,?)")) {
            p.setString(1, lich);
            p.setString(2, san);
            p.setString(3, sl[2] + " - " + sl[3]);
            p.setTimestamp(4, Timestamp.valueOf(startDt));
            p.setTimestamp(5, Timestamp.valueOf(endDt));
            String st = "direct".equals(mode) || PAID.equals(status) ? BOOKED : HOLD;
            p.setString(6, st);
            p.setDate(7, Date.valueOf(day));
            p.setString(8, sl[0]);
            p.setInt(9, ca);
            p.executeUpdate();
        }
    }

    private double calculateServiceTotal(Connection c, Map<String, Integer> services) throws SQLException {
        double serviceTotal = 0;
        if (services != null) {
            for (Map.Entry<String, Integer> entry : services.entrySet()) {
                serviceTotal += getServicePrice(c, entry.getKey()) * entry.getValue();
            }
        }
        return serviceTotal;
    }

    private void insertHoaDon(Connection c, String hd, String kh, double total, double deposit, String status) throws SQLException {
        double remaining = Math.max(0, total - deposit);
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO HoaDon(ma_hd,ma_kh,ma_nv,ngay_lap,tong_tien,tien_coc,so_tien_con_lai,han_thanh_toan,trang_thai) "
                        + "SELECT ?,?,(SELECT TOP 1 ma_nv FROM NhanVien),SYSDATETIME(),?,?,?,DATEADD(MINUTE,60,SYSDATETIME()),?")) {
            p.setString(1, hd);
            p.setString(2, kh);
            p.setDouble(3, total);
            p.setDouble(4, deposit);
            p.setDouble(5, remaining);
            p.setString(6, status);
            p.executeUpdate();
        }
    }

    private void insertCthdForSlots(Connection c, String hd, List<String> lichIds, List<Double> slotPrices) throws SQLException {
        for (int i = 0; i < lichIds.size(); i++) {
            double price = slotPrices.get(i);
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,NULL,1,?,?)")) {
                p.setString(1, hd);
                p.setString(2, lichIds.get(i));
                p.setDouble(3, price);
                p.setDouble(4, price);
                p.executeUpdate();
            }
        }
    }

    private void insertCthdForServices(Connection c, String hd, String firstLich, Map<String, Integer> services) throws SQLException {
        if (services != null && !services.isEmpty()) {
            for (Map.Entry<String, Integer> entry : services.entrySet()) {
                String svId = entry.getKey();
                int qty = entry.getValue();
                double price = getServicePrice(c, svId);
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)")) {
                    p.setString(1, hd);
                    p.setString(2, firstLich);
                    p.setString(3, svId);
                    p.setInt(4, qty);
                    p.setDouble(5, price);
                    p.setDouble(6, price * qty);
                    p.executeUpdate();
                }
            }
        }
    }

    /**
     * Lấy giá thuê của một sân.
     */
    private double getFieldPrice(Connection c, String fieldId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT gia_theo_gio FROM San WHERE ma_san=?")) {
            p.setString(1, fieldId);
            ResultSet r = p.executeQuery();
            if (r.next()) return r.getDouble(1) * 2;
        }
        return 600000;
    }

    /**
     * Lấy đơn giá của một dịch vụ.
     */
    private double getServicePrice(Connection c, String serviceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT don_gia FROM DichVu WHERE ma_dv=?")) {
            p.setString(1, serviceId);
            ResultSet r = p.executeQuery();
            if (r.next()) return r.getDouble(1);
        }
        return 0;
    }

    /**
     * Kiểm tra xem một ca sân tại ngày cụ thể có trống hay không.
     */
    private boolean isSlotFree(String san, LocalDate day, int ca) throws SQLException {
        // Chặn đặt ca đã qua giờ bắt đầu trong ngày hôm nay
        if (day.equals(LocalDate.now())) {
            try {
                String[] sl = Slots.get(ca);
                LocalTime start = LocalTime.parse(sl[2]);
                if (!start.isAfter(LocalTime.now())) return false;
            } catch (Exception ignored) { }
        }
        String sql = "SELECT COUNT(*) FROM LichSan WHERE ma_san=? AND ngay_dat=? AND so_ca=? AND trang_thai NOT IN (N'" + FREE + "', N'" + CANCEL + "')";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, san);
            p.setDate(2, Date.valueOf(day));
            p.setInt(3, ca);
            ResultSet r = p.executeQuery();
            return r.next() && r.getInt(1) == 0;
        }
    }

    /**
     * Lấy mã khách hàng dựa trên số điện thoại, hoặc tạo mới nếu chưa tồn tại.
     */
    private String customer(Connection c, String name, String phone) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT ma_kh FROM KhachHang WHERE sdt=?")) {
            p.setString(1, phone);
            ResultSet r = p.executeQuery();
            if (r.next()) return r.getString(1);
        }
        String id = next(c, "KhachHang", "ma_kh", "KH", 3);
        try (PreparedStatement p = c.prepareStatement("INSERT INTO KhachHang(ma_kh,ho_ten,sdt) VALUES(?,?,?)")) {
            p.setString(1, id);
            p.setString(2, name);
            p.setString(3, phone);
            p.executeUpdate();
        }
        return id;
    }

    /**
     * Sinh mã tự tăng tiếp theo cho một bảng.
     */
    private String next(Connection c, String table, String col, String prefix, int len) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(" + col + "," + (prefix.length() + 1) + ",20))),0)+1 FROM " + table)) {
            r.next();
            return prefix + String.format("%0" + len + "d", r.getInt(1));
        }
    }

    /**
     * Lấy chi tiết thông tin hóa đơn bao gồm thông tin khách hàng, các ca sân và dịch vụ.
     * @param id Mã hóa đơn
     * @return Map chứa toàn bộ thông tin chi tiết hóa đơn
     */
    public Map<String, Object> invoice(String id) throws SQLException {
        String sql = "SELECT h.*,k.ho_ten,k.sdt," +
                "v.ma_code,v.ten_voucher,v.loai_giam,v.gia_tri,v.don_toi_thieu,v.loai AS voucher_loai " +
                "FROM HoaDon h " +
                "JOIN KhachHang k ON k.ma_kh=h.ma_kh " +
                "LEFT JOIN Voucher v ON v.ma_voucher=h.ma_voucher " +
                "WHERE h.ma_hd=? AND h.trang_thai <> N'ĐÃ XÓA'";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            List<Map<String, Object>> list = rows(p.executeQuery());
            if (!list.isEmpty()) {
                Map<String, Object> inv = list.get(0);
                
                // Tính toán tiền giảm giá để hiển thị
                double rawTotal = 0;
                try (PreparedStatement pSum = c.prepareStatement("SELECT SUM(thanh_tien) FROM CTHD WHERE ma_hd=?")) {
                    pSum.setString(1, id);
                    ResultSet rsSum = pSum.executeQuery();
                    if (rsSum.next()) rawTotal = rsSum.getDouble(1);
                }
                inv.put("raw_total", rawTotal);
                Object tg = inv.get("tien_giam");
                double discount = tg != null ? ((Number) tg).doubleValue()
                        : rawTotal - ((Number) inv.get("tong_tien")).doubleValue();
                inv.put("discount_amount", discount);
                inv.put("tien_giam", discount);
                
                // Lấy danh sách các ca sân trong chi tiết hóa đơn
                String sqlSlots = "SELECT s.ma_san, s.ten_san,s.loai_san,l.ngay_dat,l.buoi,l.ca,l.so_ca, "
                        + "CONVERT(VARCHAR(5),l.gio_bat_dau,108) gio_bat_dau, "
                        + "CONVERT(VARCHAR(5),l.gio_ket_thuc,108) gio_ket_thuc, d.thanh_tien "
                        + "FROM CTHD d JOIN LichSan l ON l.ma_lich=d.ma_lich "
                        + "JOIN San s ON s.ma_san=l.ma_san WHERE d.ma_hd=? AND d.ma_dv IS NULL";
                try (PreparedStatement ps = c.prepareStatement(sqlSlots)) {
                    ps.setString(1, id);
                    inv.put("slots", rows(ps.executeQuery()));
                }

                // Lấy danh sách các dịch vụ kèm theo
                List<Map<String, Object>> svcs = new ArrayList<>();
                String sql2 = "SELECT c.ma_dv, dv.ten_dv, c.so_luong, c.don_gia, c.thanh_tien "
                            + "FROM CTHD c LEFT JOIN DichVu dv ON dv.ma_dv=c.ma_dv "
                            + "WHERE c.ma_hd=? AND c.ma_dv IS NOT NULL";
                try (PreparedStatement p2 = c.prepareStatement(sql2)) {
                    p2.setString(1, id);
                    ResultSet rs2 = p2.executeQuery();
                    while (rs2.next()) {
                        Map<String, Object> sv = new LinkedHashMap<>();
                        sv.put("ma_dv", rs2.getString("ma_dv"));
                        sv.put("ten_dv", rs2.getString("ten_dv"));
                        sv.put("so_luong", rs2.getInt("so_luong"));
                        sv.put("don_gia", rs2.getDouble("don_gia"));
                        sv.put("thanh_tien", rs2.getDouble("thanh_tien"));
                        svcs.add(sv);
                    }
                }
                inv.put("services", svcs);
                return inv;
            }
            return null;
        }
    }

    /**
     * Xử lý thanh toán tiền cọc cho hóa đơn.
     */
    public boolean pay(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE HoaDon SET trang_thai=?, so_tien_con_lai=tong_tien-tien_coc WHERE ma_hd=? AND trang_thai=? AND han_thanh_toan>SYSDATETIME()")) {
                    p.setString(1, DEPOSITED);
                    p.setString(2, id);
                    p.setString(3, UNPAID);
                    if (p.executeUpdate() != 1) {
                        c.rollback();
                        return false;
                    }
                }
                
                // Giảm số lượng voucher khi sử dụng thành công
                useVoucher(c, id);

                // Cập nhật trạng thái các ca sân liên quan
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE l SET trang_thai=? FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich WHERE d.ma_hd=?")) {
                    p.setString(1, BOOKED);
                    p.setString(2, id);
                    p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Xử lý thanh toán phần tiền còn lại của hóa đơn.
     */
    public boolean payRemaining(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String oldStatus = "";
                try (PreparedStatement p = c.prepareStatement("SELECT trang_thai FROM HoaDon WHERE ma_hd=?")) {
                    p.setString(1, id);
                    ResultSet rs = p.executeQuery();
                    if (rs.next()) oldStatus = rs.getString(1);
                }

                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE HoaDon SET trang_thai=?, so_tien_con_lai=0 WHERE ma_hd=? AND (trang_thai=? OR trang_thai=? OR trang_thai=?)")) {
                    p.setString(1, PAID);
                    p.setString(2, id);
                    p.setString(3, UNPAID);
                    p.setString(4, DEPOSITED);
                    p.setString(5, BOOKED);
                    int n = p.executeUpdate();
                    if (n == 0) { c.rollback(); return false; }
                }

                // Nếu từ UNPAID lên thẳng PAID, giảm số lượng voucher
                if (UNPAID.equals(oldStatus)) {
                    useVoucher(c, id);
                }

                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE l SET trang_thai=? FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich WHERE d.ma_hd=?")) {
                    p.setString(1, BOOKED);
                    p.setString(2, id);
                    p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void useVoucher(Connection c, String hdId) throws SQLException {
        // Ưu tiên schema mới (so_luong / ma_voucher); fallback schema cũ nếu còn
        try (PreparedStatement p = c.prepareStatement(
                "UPDATE Voucher SET so_luong = so_luong - 1 " +
                "WHERE ma_voucher = (SELECT ma_voucher FROM HoaDon WHERE ma_hd=?) AND so_luong > 0")) {
            p.setString(1, hdId);
            if (p.executeUpdate() > 0) return;
        } catch (SQLException ignored) { }
        try (PreparedStatement p = c.prepareStatement(
                "UPDATE Voucher SET soLuong = soLuong - 1 " +
                "WHERE idVoucher = (SELECT ma_voucher FROM HoaDon WHERE ma_hd=?)")) {
            p.setString(1, hdId);
            p.executeUpdate();
        } catch (SQLException ignored) { }
    }

    /**
     * Hủy bỏ hóa đơn và giải phóng các ca sân đã chọn.
     */
    public boolean cancelInvoice(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("UPDATE HoaDon SET trang_thai=? WHERE ma_hd=?")) {
                p.setString(1, CANCEL);
                p.setString(2, id);
                p.executeUpdate();
            }
            // Giải phóng các ca sân về trạng thái "CÒN TRỐNG"
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE l SET trang_thai=? FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich WHERE d.ma_hd=?")) {
                p.setString(1, FREE);
                p.setString(2, id);
                p.executeUpdate();
            }
            c.commit();
            return true;
        }
    }

    /**
     * Lấy dữ liệu hóa đơn để phục vụ chức năng chỉnh sửa.
     */
    public Map<String, Object> invoiceForEdit(String id) throws SQLException {
        String sql = "SELECT h.ma_hd, h.tong_tien, h.tien_coc, h.so_tien_con_lai, h.trang_thai, " +
                "k.ma_kh, k.ho_ten, k.sdt, s.ma_san, l.ma_lich, l.so_ca, l.ngay_dat " +
                "FROM HoaDon h JOIN KhachHang k ON k.ma_kh=h.ma_kh " +
                "JOIN CTHD d ON d.ma_hd=h.ma_hd " +
                "JOIN LichSan l ON l.ma_lich=d.ma_lich " +
                "JOIN San s ON s.ma_san=l.ma_san WHERE h.ma_hd=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            ResultSet r = p.executeQuery();
            if (!r.next()) return null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma_hd", r.getString("ma_hd"));
            m.put("ho_ten", r.getString("ho_ten"));
            m.put("sdt", r.getString("sdt"));
            m.put("ma_san", r.getString("ma_san"));
            m.put("so_ca", r.getInt("so_ca"));
            m.put("ngay_dat", r.getDate("ngay_dat"));

            // Lấy danh sách số lượng dịch vụ đã chọn trong hóa đơn
            Map<String, Integer> qty = new LinkedHashMap<>();
            try (PreparedStatement p2 = c.prepareStatement("SELECT ma_dv, so_luong FROM CTHD WHERE ma_hd=? AND ma_dv IS NOT NULL")) {
                p2.setString(1, id);
                ResultSet r2 = p2.executeQuery();
                while (r2.next()) qty.put(r2.getString(1), r2.getInt(2));
            }
            m.put("serviceQty", qty);
            m.put("serviceIds", new ArrayList<>(qty.keySet()));
            return m;
        }
    }

    /**
     * Cập nhật thông tin một hóa đơn đã tồn tại.
     */
    public void updateInvoice(String invoiceId, String[] fields, LocalDate date, int[] cas, String name, String phone, Map<String, Integer> services) throws Exception {
        validateBookingData(fields, cas);

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String, Object> currentInfo = getInvoiceSummaryForUpdate(c, invoiceId);
                String maKh = (String) currentInfo.get("ma_kh");
                String status = (String) currentInfo.get("trang_thai");
                double tienCoc = (double) currentInfo.get("tien_coc");

                updateCustomerInfo(c, maKh, name, phone);
                clearOldInvoiceDetails(c, invoiceId);

                List<String> newLichIds = new ArrayList<>();
                List<Double> slotPrices = new ArrayList<>();
                double baseTotal = processBookingSlots(c, fields, date, cas, null, status, newLichIds, slotPrices);

                double serviceTotal = calculateServiceTotal(c, services);
                insertCthdForSlots(c, invoiceId, newLichIds, slotPrices);
                insertCthdForServices(c, invoiceId, newLichIds.get(0), services);

                double total = baseTotal + serviceTotal;
                updateHoaDonTotals(c, invoiceId, total, status, tienCoc);

                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private Map<String, Object> getInvoiceSummaryForUpdate(Connection c, String invoiceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT ma_kh, trang_thai, tien_coc FROM HoaDon WHERE ma_hd=?")) {
            p.setString(1, invoiceId);
            ResultSet r = p.executeQuery();
            if (!r.next()) throw new SQLException("Không tìm thấy hóa đơn");
            Map<String, Object> info = new HashMap<>();
            info.put("ma_kh", r.getString(1));
            info.put("trang_thai", r.getString(2));
            info.put("tien_coc", r.getDouble(3));
            return info;
        }
    }

    private void updateCustomerInfo(Connection c, String maKh, String name, String phone) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("UPDATE KhachHang SET ho_ten=?, sdt=? WHERE ma_kh=?")) {
            p.setString(1, name);
            p.setString(2, phone);
            p.setString(3, maKh);
            p.executeUpdate();
        }
    }

    private void clearOldInvoiceDetails(Connection c, String invoiceId) throws SQLException {
        List<String> oldLichIds = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT DISTINCT ma_lich FROM CTHD WHERE ma_hd=? AND ma_lich IS NOT NULL")) {
            p.setString(1, invoiceId);
            ResultSet r = p.executeQuery();
            while (r.next()) oldLichIds.add(r.getString(1));
        }

        try (PreparedStatement p = c.prepareStatement("DELETE FROM CTHD WHERE ma_hd=?")) {
            p.setString(1, invoiceId);
            p.executeUpdate();
        }

        for (String lid : oldLichIds) {
            try (PreparedStatement p = c.prepareStatement("DELETE FROM LichSan WHERE ma_lich=?")) {
                p.setString(1, lid);
                p.executeUpdate();
            }
        }
    }

    private void updateHoaDonTotals(Connection c, String invoiceId, double total, String status, double tienCoc) throws SQLException {
        double remain = PAID.equals(status) ? 0 : Math.max(0, total - tienCoc);
        try (PreparedStatement p = c.prepareStatement("UPDATE HoaDon SET tong_tien=?, so_tien_con_lai=? WHERE ma_hd=?")) {
            p.setDouble(1, total);
            p.setDouble(2, remain);
            p.setString(3, invoiceId);
            p.executeUpdate();
        }
    }

    /**
     * Cập nhật trạng thái hóa đơn sang "ĐANG XỬ LÝ".
     */
    public boolean setProcessing(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE HoaDon SET trang_thai=? WHERE ma_hd=? AND trang_thai=?")) {
                p.setString(1, PROCESSING);
                p.setString(2, id);
                p.setString(3, UNPAID);
                return p.executeUpdate() > 0;
            }
        }
    }

    /**
     * Lấy danh sách các lịch đặt sân của một sân trong ngày để hiển thị bảng lịch.
     */
    public List<Map<String, Object>> getBookingsByField(String fieldId, LocalDate date) throws SQLException {
        String sql = "SELECT l.ma_lich, l.ma_san, s.ten_san, l.trang_thai, l.ngay_dat, l.so_ca, "
                + "l.buoi, l.ca, "
                + "k.ho_ten ten_khach, k.sdt, "
                + "CASE WHEN l.trang_thai=N'"+HOLD+"' THEN 1 WHEN l.trang_thai=N'"+BOOKED+"' THEN 2 "
                + "WHEN l.trang_thai=N'"+EXPIRED+"' THEN 3 WHEN l.trang_thai=N'"+EVENT+"' THEN 4 ELSE 0 END status_code "
                + "FROM LichSan l JOIN San s ON s.ma_san=l.ma_san "
                + "JOIN CTHD c ON c.ma_lich=l.ma_lich "
                + "JOIN HoaDon h ON h.ma_hd=c.ma_hd "
                + "JOIN KhachHang k ON k.ma_kh=h.ma_kh "
                + "WHERE l.ma_san=? AND l.ngay_dat=? "
                + "ORDER BY l.so_ca";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, fieldId);
            p.setDate(2, Date.valueOf(date));
            return rows(p.executeQuery());
        }
    }

    /**
     * Lấy danh sách các hóa đơn liên quan đến một sân cụ thể.
     */
    public List<Map<String, Object>> getInvoicesByField(String fieldId) throws SQLException {
        String sql = "SELECT h.ma_hd, h.ma_kh, h.tong_tien, h.ngay_lap ngay_xuat, h.da_thu, "
                + "k.ho_ten ten_khach "
                + "FROM HoaDon h JOIN CTHD d ON d.ma_hd=h.ma_hd "
                + "JOIN LichSan l ON l.ma_lich=d.ma_lich "
                + "JOIN KhachHang k ON k.ma_kh=h.ma_kh "
                + "WHERE l.ma_san=? "
                + "ORDER BY h.ngay_lap DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, fieldId);
            return rows(p.executeQuery());
        }
    }

    /**
     * Thống kê số lượng ca đã đặt, đang chờ, hết và còn trống trong ngày hôm nay của tất cả các sân.
     */
    public Map<String, Integer> fieldStats() throws SQLException {
        List<Map<String, Object>> fields = fields();
        LocalDate today = LocalDate.now();
        int totalSlots = 0;
        int bookedCount = 0;
        int pendingCount = 0;
        int expiredCount = 0;
        for (Map<String, Object> f : fields) {
            List<Map<String, Object>> slotList = slots(String.valueOf(f.get("ma_san")), today);
            for (Map<String, Object> s : slotList) {
                totalSlots++;
                int code = ((Number) s.get("status_code")).intValue();
                if (code == 1) pendingCount++;
                else if (code == 2) bookedCount++;
                else if (code == 3) expiredCount++;
            }
        }
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("booked", bookedCount);
        stats.put("pending", pendingCount);
        stats.put("expired", expiredCount);
        stats.put("free", totalSlots - bookedCount - pendingCount - expiredCount);
        return stats;
    }

    /**
     * Tự động hủy các hóa đơn quá hạn thanh toán và giải phóng các ca sân tương ứng.
     */
    public void expire() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            // Hủy hóa đơn chưa thanh toán quá hạn
            s.execute("UPDATE h SET trang_thai=N'" + CANCEL + "' FROM HoaDon h WHERE h.trang_thai=N'" + UNPAID + "' AND h.han_thanh_toan<SYSDATETIME()");
            // Giải phóng các lịch sân thuộc các hóa đơn đã bị hủy
            s.execute("UPDATE l SET trang_thai=N'" + FREE + "' FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich JOIN HoaDon h ON h.ma_hd=d.ma_hd WHERE h.trang_thai=N'" + CANCEL + "'");
        }
    }

    /**
     * Tự động đặt trạng thái "HẾT" cho các ca trống có giờ bắt đầu đã qua thời điểm hiện tại.
     */
    public void expireSlots() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            // Đặt trạng thái "HẾT" cho các ca trống có giờ bắt đầu đã qua thời điểm hiện tại
            s.execute("UPDATE LichSan SET trang_thai=N'" + EXPIRED + "' "
                    + "WHERE trang_thai IN (N'" + FREE + "', N'" + CANCEL + "') "
                    + "AND gio_bat_dau <= SYSDATETIME()");
        }
    }

    /**
     * Xóa các hóa đơn trống (không có bất kỳ chi tiết nào trong CTHD).
     */
    public void deleteEmptyInvoices() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM HoaDon WHERE ma_hd NOT IN (SELECT DISTINCT ma_hd FROM CTHD)");
        }
    }

    /**
     * Đảm bảo luôn có một hóa đơn hoạt động để lưu trữ các lựa chọn của người dùng.
     */
    public String ensureInvoice(String hdId, String name, String phone) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (hdId != null && !hdId.isBlank()) {
                    try (PreparedStatement p = c.prepareStatement("SELECT ma_hd FROM HoaDon WHERE ma_hd=? AND trang_thai <> N'ĐÃ XÓA'")) {
                        p.setString(1, hdId);
                        if (p.executeQuery().next()) {
                            // Nếu có thông tin khách hàng mới, cập nhật vào hóa đơn hiện tại
                            if (name != null && !name.isBlank() && phone != null && !phone.isBlank()) {
                                String maKh = customer(c, name, phone);
                                try (PreparedStatement p2 = c.prepareStatement("UPDATE HoaDon SET ma_kh=? WHERE ma_hd=?")) {
                                    p2.setString(1, maKh);
                                    p2.setString(2, hdId);
                                    p2.executeUpdate();
                                }
                            }
                            c.commit();
                            return hdId;
                        }
                    }
                }
                // Tạo mới khách hàng "vãng lai" nếu chưa có thông tin
                String kh = customer(c, (name == null || name.isBlank()) ? "Khách" : name,
                        (phone == null || phone.isBlank()) ? "0000000000" : phone);
                String hd = next(c, "HoaDon", "ma_hd", "HD", 6);
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO HoaDon(ma_hd,ma_kh,ma_nv,ngay_lap,tong_tien,tien_coc,so_tien_con_lai,han_thanh_toan,trang_thai) "
                                + "SELECT ?,?,(SELECT TOP 1 ma_nv FROM NhanVien),SYSDATETIME(),0,0,0,DATEADD(MINUTE,60,SYSDATETIME()),?")) {
                    p.setString(1, hd);
                    p.setString(2, kh);
                    p.setString(3, UNPAID);
                    p.executeUpdate();
                }
                c.commit();
                return hd;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Bật/Tắt lựa chọn một ca sân trong hóa đơn nháp.
     */
    public void toggleSlot(String hdId, String fieldId, LocalDate date, int ca) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String lichId = findLichId(c, hdId, fieldId, date, ca);

                if (lichId != null) {
                    removeSlot(c, hdId, lichId);
                } else {
                    addSlot(c, hdId, fieldId, date, ca);
                }
                c.commit();
                updateTotals(hdId);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private String findLichId(Connection c, String hdId, String fieldId, LocalDate date, int ca) throws SQLException {
        String sql = "SELECT l.ma_lich FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich " +
                "WHERE d.ma_hd=? AND l.ma_san=? AND l.ngay_dat=? AND l.so_ca=?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, hdId);
            p.setString(2, fieldId);
            p.setDate(3, Date.valueOf(date));
            p.setInt(4, ca);
            ResultSet r = p.executeQuery();
            return r.next() ? r.getString(1) : null;
        }
    }

    private void removeSlot(Connection c, String hdId, String lichId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("DELETE FROM CTHD WHERE ma_hd=? AND ma_lich=?")) {
            p.setString(1, hdId);
            p.setString(2, lichId);
            p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement("DELETE FROM LichSan WHERE ma_lich=?")) {
            p.setString(1, lichId);
            p.executeUpdate();
        }
    }

    private void addSlot(Connection c, String hdId, String fieldId, LocalDate date, int ca) throws Exception {
        if (!isSlotFree(fieldId, date, ca)) throw new Exception("Sân đã được đặt hoặc đang chờ");

        String newLich = next(c, "LichSan", "ma_lich", "L", 3);
        insertLichSanRecord(c, newLich, fieldId, ca, date, null, HOLD);

        double pr = getFieldPrice(c, fieldId);
        try (PreparedStatement p = c.prepareStatement("INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,NULL,1,?,?)")) {
            p.setString(1, hdId);
            p.setString(2, newLich);
            p.setDouble(3, pr);
            p.setDouble(4, pr);
            p.executeUpdate();
        }
    }

    /**
     * Cập nhật số lượng hoặc thêm mới dịch vụ vào hóa đơn.
     */
    public void updateService(String hdId, String serviceId, int qty) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String lichId = findAnyLichIdForInvoice(c, hdId);
                if (lichId == null) throw new SQLException("Vui lòng chọn sân trước khi thêm dịch vụ");

                int existingQty = getExistingServiceQty(c, hdId, serviceId);
                if (existingQty > 0) {
                    if (qty <= 0) {
                        // Xóa dịch vụ nếu số lượng mới <= 0
                        removeServiceFromInvoice(c, hdId, serviceId);
                    } else {
                        // Cộng thêm số lượng mới vào số lượng hiện có
                        int newQty = existingQty + qty;
                        double price = getServicePrice(c, serviceId);
                        try (PreparedStatement p = c.prepareStatement(
                                "UPDATE CTHD SET so_luong=?, thanh_tien=? WHERE ma_hd=? AND ma_dv=?")) {
                            p.setInt(1, newQty);
                            p.setDouble(2, price * newQty);
                            p.setString(3, hdId);
                            p.setString(4, serviceId);
                            p.executeUpdate();
                        }
                    }
                } else if (qty > 0) {
                    // Thêm mới dịch vụ
                    addServiceToInvoice(c, hdId, lichId, serviceId, qty);
                }
                c.commit();
                updateTotals(hdId);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Lấy số lượng dịch vụ hiện có trong hóa đơn.
     */
    private int getExistingServiceQty(Connection c, String hdId, String serviceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "SELECT so_luong FROM CTHD WHERE ma_hd=? AND ma_dv=?")) {
            p.setString(1, hdId);
            p.setString(2, serviceId);
            ResultSet r = p.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private String findAnyLichIdForInvoice(Connection c, String hdId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT TOP 1 ma_lich FROM CTHD WHERE ma_hd=? AND ma_lich IS NOT NULL")) {
            p.setString(1, hdId);
            ResultSet r = p.executeQuery();
            return r.next() ? r.getString(1) : null;
        }
    }

    private void removeServiceFromInvoice(Connection c, String hdId, String serviceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("DELETE FROM CTHD WHERE ma_hd=? AND ma_dv=?")) {
            p.setString(1, hdId);
            p.setString(2, serviceId);
            p.executeUpdate();
        }
    }

    private void addServiceToInvoice(Connection c, String hdId, String lichId, String serviceId, int qty) throws SQLException {
        double price = getServicePrice(c, serviceId);
        try (PreparedStatement p = c.prepareStatement("INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, hdId);
            p.setString(2, lichId);
            p.setString(3, serviceId);
            p.setInt(4, qty);
            p.setDouble(5, price);
            p.setDouble(6, price * qty);
            p.executeUpdate();
        }
    }

    /**
     * Lấy danh sách Voucher khả dụng cho Booking (Hóa đơn).
     */
    public List<Map<String, Object>> getAvailableVouchers(String hdId) throws SQLException {
        double totalBeforeVoucher = 0;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT ISNULL(SUM(thanh_tien),0) FROM CTHD WHERE ma_hd=?")) {
            p.setString(1, hdId);
            ResultSet r = p.executeQuery();
            if (r.next()) totalBeforeVoucher = r.getDouble(1);
        }
        VoucherRepository voucherRepo = new VoucherRepository();
        return voucherRepo.toMapList(voucherRepo.getAvailableHoaDonVouchers(totalBeforeVoucher));
    }

    /**
     * Áp dụng Voucher cho hóa đơn.
     */
    public void applyVoucher(String hdId, String voucherId) throws Exception {
        if (voucherId == null || voucherId.isBlank()) {
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement p = c.prepareStatement(
                         "UPDATE HoaDon SET ma_voucher=NULL, tien_giam=0 WHERE ma_hd=?")) {
                p.setString(1, hdId);
                p.executeUpdate();
            }
            updateTotals(hdId);
            return;
        }
        VoucherRepository voucherRepo = new VoucherRepository();
        com.footballbooking.model.Entities.Voucher v = voucherRepo.getById(voucherId);
        if (v == null) v = voucherRepo.getByCode(voucherId);
        if (v == null) throw new IllegalArgumentException("Voucher không tồn tại");
        if (v.getLoai() != com.footballbooking.model.Entities.VoucherLoai.HOA_DON) {
            throw new IllegalArgumentException("Voucher này không áp dụng cho Hóa đơn (Booking)");
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE HoaDon SET ma_voucher=? WHERE ma_hd=?")) {
            p.setString(1, v.getMaVoucher());
            p.setString(2, hdId);
            p.executeUpdate();
        }
        updateTotals(hdId);
    }

    private boolean isVoucherValid(Connection c, String voucherId, String hdId, double rawTotal) throws SQLException {
        VoucherRepository voucherRepo = new VoucherRepository();
        com.footballbooking.model.Entities.Voucher v = voucherRepo.getById(voucherId);
        if (v == null) return false;

        // Chỉ voucher Hóa đơn (loai = 1) mới áp dụng trên Booking
        if (v.getLoai() != com.footballbooking.model.Entities.VoucherLoai.HOA_DON) return false;

        LocalDate today = LocalDate.now();
        if (v.getNgayBatDau() != null && today.isBefore(v.getNgayBatDau())) return false;
        if (v.getNgayKetThuc() != null && today.isAfter(v.getNgayKetThuc())) return false;
        if (v.getSoLuong() <= 0) return false;
        if (rawTotal < v.getDonToiThieu()) return false;

        String st = v.getTrangThai();
        if (st != null && !st.isBlank()) {
            String up = st.trim().toUpperCase();
            if (up.contains("NGỪNG") || up.contains("KHÓA") || "0".equals(up) || "INACTIVE".equals(up) || "FALSE".equals(up)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tính toán lại tổng tiền, tiền cọc, tiền giảm và số tiền còn lại cho hóa đơn.
     */
    private void updateTotals(String hdId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            double rawTotal = 0;
            try (PreparedStatement p = c.prepareStatement("SELECT ISNULL(SUM(thanh_tien),0) FROM CTHD WHERE ma_hd=?")) {
                p.setString(1, hdId);
                ResultSet r = p.executeQuery();
                if (r.next()) rawTotal = r.getDouble(1);
            }

            String voucherId = null;
            try (PreparedStatement p = c.prepareStatement("SELECT ma_voucher FROM HoaDon WHERE ma_hd=?")) {
                p.setString(1, hdId);
                ResultSet r = p.executeQuery();
                if (r.next()) voucherId = r.getString(1);
            }

            double discount = 0;
            if (voucherId != null && !voucherId.isBlank()) {
                if (isVoucherValid(c, voucherId, hdId, rawTotal)) {
                    VoucherRepository voucherRepo = new VoucherRepository();
                    com.footballbooking.model.Entities.Voucher v = voucherRepo.getById(voucherId);
                    if (v != null) {
                        String loaiGiam = v.getLoaiGiam() != null ? v.getLoaiGiam().trim().toUpperCase() : "MONEY";
                        double giaTri = v.getGiaTri();
                        if ("PERCENT".equals(loaiGiam) || "GIAM_THEO_PHAN_TRAM".equals(loaiGiam)) {
                            discount = rawTotal * (giaTri / 100.0);
                        } else {
                            // MONEY / GIAM_TRUC_TIEP
                            discount = giaTri;
                        }
                        if (discount > rawTotal) discount = rawTotal;
                    }
                } else {
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE HoaDon SET ma_voucher=NULL, tien_giam=0 WHERE ma_hd=?")) {
                        p.setString(1, hdId);
                        p.executeUpdate();
                    }
                    voucherId = null;
                }
            }

            double finalTotal = Math.max(0, rawTotal - discount);
            double deposit = finalTotal * 0.3;
            double remain = finalTotal - deposit;

            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE HoaDon SET tong_tien=?, tien_coc=?, so_tien_con_lai=?, tien_giam=? WHERE ma_hd=?")) {
                p.setDouble(1, finalTotal);
                p.setDouble(2, deposit);
                p.setDouble(3, remain);
                p.setDouble(4, discount);
                p.setString(5, hdId);
                p.executeUpdate();
            }
        }
    }

    /**
     * Thực thi một câu lệnh truy vấn SQL trả về danh sách các bản ghi.
     */
    private List<Map<String, Object>> query(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            return rows(s.executeQuery(sql));
        }
    }

    /**
     * Chuyển đổi ResultSet từ SQL sang danh sách các Map (dạng key-value).
     */
    private List<Map<String, Object>> rows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }
}
```

**Lưu ý quan trọng:** BookingRepository có 1189 dòng, là class dùng chung cho nhiều chức năng. Người 2 cần đọc toàn bộ để hiểu luồng đặt sân.

---

## 8. CÁC JSP

### 8.1 booking.jsp (Trang đặt sân)
**Đường dẫn:** `/jsp/user/booking.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="dayNames" value="Thứ 2,Thứ 3,Thứ 4,Thứ 5,Thứ 6,Thứ 7,Chủ nhật"/>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.booking-layout{display:flex;gap:16px;align-items:flex-start;max-width:1600px;margin:0 auto;}
.booking-form-panel{flex:0 0 320px;position:sticky;top:10px;}
.booking-table-panel{flex:1;min-width:0;}
.booking-table table{font-size:13px;border-collapse:collapse;width:100%;}
.booking-table th,.booking-table td{border:1px solid #ccc;padding:8px;text-align:center;vertical-align:top;min-width:90px;}
.booking-table th{background:#f5f5f5;font-weight:bold;}
.booking-table thead tr:first-child th{background:#1565c0;color:#fff;}
.booking-table tbody th{background:#1565c0;color:#fff;}
.booking-table td.free{background:#fff;color:#333;cursor:pointer;transition:all 0.2s ease;}
.booking-table td.free:hover{background:#e3f2fd;transform:scale(1.02);border-color:#2196f3;box-shadow:0 2px 8px rgba(0,0,0,0.1);}
.booking-table td.selected{background:#2196f3;color:#fff;font-weight:bold;box-shadow:0 0 10px rgba(33,150,243,0.5);animation:glow 0.5s ease;}
.booking-table td.holding{background:#ffd54f;color:#333;cursor:not-allowed;font-style:italic;}
.booking-table td.pending{background:#ffd54f;color:#333;cursor:not-allowed;}
.booking-table td.booked{background:#ef5350;color:#fff;cursor:not-allowed;}
.booking-table td.event{background:#ce93d8;color:#333;cursor:not-allowed;}
.booking-table td.expired{background:#9e9e9e;color:#fff;cursor:not-allowed;}
.service-menu-table{width:100%;border-collapse:collapse;margin-top:10px;font-size:12px;}
.service-menu-table th,.service-menu-table td{border:1px solid #eee;padding:6px;text-align:left;}
.service-menu-table th{background:#f9f9f9;}
.invoice-detail-section{margin-top:20px;padding:15px;border:2px dashed #1565c0;background:#fff;}
.invoice-table{width:100%;border-collapse:collapse;margin-top:10px;font-size:13px;}
.invoice-table th,.invoice-table td{padding:8px;border-bottom:1px solid #eee;text-align:left;}
.invoice-table th{color:#1565c0;font-weight:bold;}
.invoice-total{margin-top:10px;text-align:right;font-size:15px;}
.badge-status{padding:2px 6px;border-radius:10px;font-size:10px;font-weight:bold;text-transform:uppercase;}
.status-free{background:#e8f5e9;color:#2e7d32;}
.status-holding{background:#fff8e1;color:#f57f17;}
.status-booked{background:#ffebee;color:#c62828;}
.status-event{background:#f3e5f5;color:#7b1fa2;}
.status-expired{background:#eeeeee;color:#616161;}
@keyframes glow{0%{box-shadow:0 0 0 0 rgba(33,150,243,0.7);}50%{box-shadow:0 0 0 5px rgba(33,150,243,0.3);}100%{box-shadow:0 0 10px rgba(33,150,243,0.5);}}
</style>
<div style="padding:0 12px;">
<p><a class="btn" href="${pageContext.request.contextPath}/home">&larr; Về</a>
<input type="date" id="booking-date" min="${today}" value="${viewDate}" style="width:auto;"></p>
<h3>Đặt lịch</h3>
<c:if test="${not empty bookingError}"><div class="alert">${bookingError}</div><c:remove var="bookingError" scope="session"/></c:if>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<c:if test="${not empty activeInvoiceId}">
<p style="background:#e3f2fd;padding:8px 12px;border-radius:4px;border-left:4px solid #2196f3;">
<b>Đang thao tác trên hóa đơn:</b> ${activeInvoiceId}
</p>
</c:if>

<c:choose>
<c:when test="${empty fields}"><div class="alert">Chưa có dữ liệu sân.</div></c:when>
<c:otherwise>
<div class="booking-layout">
<div class="booking-form-panel">
<div style="border:1px solid #999;padding:12px;background:#fff;">
<h4>Thông tin khách hàng</h4>
<form id="booking-confirm-form" action="${pageContext.request.contextPath}/booking" method="post">
<input type="hidden" name="op" value="confirm">
<input type="hidden" name="date" value="${viewDate}">

<label>Họ và tên <span style="color:red">*</span></label>
<input type="text" name="name" value="${not empty editName ? editName : ''}" placeholder="Họ tên" required style="width:100%;padding:8px;margin:4px 0;">
<label>Số điện thoại <span style="color:red">*</span></label>
<input type="tel" name="phone" value="${not empty editPhone ? editPhone : ''}" placeholder="Số điện thoại" required style="width:100%;padding:8px;margin:4px 0;">
</form>

<div class="services-menu" style="margin:15px 0;">
<b>Dịch vụ:</b>
<table class="service-menu-table">
<thead><tr><th>Tên</th><th>Giá</th><th>SL</th><th></th></tr></thead>
<tbody>
<c:forEach items="${services}" var="sv">
<tr>
    <td>${sv.ten_dv}</td>
    <td><fmt:formatNumber value="${sv.don_gia}" type="number"/></td>
    <td style="padding:4px;">
        <input type="number" form="svc_form_${sv.ma_dv}" name="qty" value="1" min="1" max="50" style="width:35px;padding:2px;">
    </td>
    <td>
        <form id="svc_form_${sv.ma_dv}" action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;">
            <input type="hidden" name="op" value="addService">
            <input type="hidden" name="date" value="${viewDate}">
            <input type="hidden" name="serviceId" value="${sv.ma_dv}">
            <button type="submit" style="padding:2px 5px;font-size:10px;">Thêm</button>
        </form>
    </td>
</tr>
</c:forEach>
</tbody>
</table>
</div>

<p style="margin-top:10px;">
    <button type="submit" form="booking-confirm-form" class="btn btn-primary" style="width:100%;padding:10px;">Xác nhận đặt sân</button>
</p>
</div>
</div>

<div class="booking-table-panel">
<div style="overflow-x:auto;">
<table class="booking-table" id="schedule-table">
<thead><tr>
<th>Sân</th>
<c:forEach items="${headers}" var="h">
<th>${fn:substring(h.label,0,2)}h-${fn:substring(h.label,6,8)}h<br><small>Ca ${h.so_ca}</small></th>
</c:forEach>
</tr></thead>
<tbody>
<c:forEach items="${fields}" var="f">
<tr>
<th>${f.ten_san}</th>
<c:set var="rowSlots" value="${schedule[f.ma_san]}"/>
<c:forEach items="${rowSlots}" var="s">
    <c:set var="sc" value="${s.status_code}"/>
    <c:set var="isSelected" value="false"/>
    <c:if test="${not empty invoice.slots}">
        <c:forEach items="${invoice.slots}" var="islot">
            <c:if test="${islot.ma_san == f.ma_san && islot.so_ca == s.so_ca && islot.ngay_dat == viewDate}">
                <c:set var="isSelected" value="true"/>
            </c:if>
        </c:forEach>
    </c:if>
    
    <td class="${isSelected ? 'selected' : (sc==0?'free':sc==1?'pending':sc==2?'booked':sc==3?'expired':'event')}" style="padding:0;">
        <c:choose>
            <c:when test="${sc==0 || isSelected}">
                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;height:100%;">
                    <input type="hidden" name="op" value="selectSlot">
                    <input type="hidden" name="fieldId" value="${f.ma_san}">
                    <input type="hidden" name="slot" value="${s.so_ca}">
                    <input type="hidden" name="date" value="${viewDate}">
                    <button type="submit" style="background:none;border:none;color:inherit;width:100%;height:100%;cursor:pointer;padding:8px;display:block;font-size:inherit;">
                        ${s.nhan_ca}
                        <div style="margin-top:4px;">
                            <c:choose>
                                <c:when test="${isSelected}"><span class="badge-status status-holding" style="background:#fff;color:#2196f3;">Đang chọn</span></c:when>
                                <c:otherwise><span class="badge-status status-free">Trống</span></c:otherwise>
                            </c:choose>
                        </div>
                    </button>
                </form>
            </c:when>
            <c:otherwise>
                <div style="padding:8px;">
                    ${s.nhan_ca}
                    <div style="margin-top:4px;">
                        <c:choose>
                            <c:when test="${sc==1}"><span class="badge-status status-holding">Chờ</span></c:when>
                            <c:when test="${sc==2}"><span class="badge-status status-booked">Hết</span></c:when>
                            <c:when test="${sc==3}"><span class="badge-status status-expired">Hết</span></c:when>
                            <c:otherwise><span class="badge-status status-event">Sự kiện</span></c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </td>
</c:forEach>
</tr>
</c:forEach>
</tbody></table></div>

<div class="invoice-detail-section" id="invoice-detail">
    <h4 style="margin-top:0;color:#1565c0;border-bottom:2px solid #1565c0;padding-bottom:5px;">Chi tiết hóa đơn tạm tính</h4>
    <c:choose>
        <c:when test="${empty invoice.slots && empty invoice.services}">
            <p style="text-align:center;color:#999;">Chưa chọn hạng mục nào.</p>
        </c:when>
        <c:otherwise>
            <table class="invoice-table">
                <thead><tr><th>Hạng mục</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th><th></th></tr></thead>
                <tbody>
                    <c:forEach items="${invoice.slots}" var="s">
                        <tr>
                            <td>Sân ${s.ten_san} - Ca ${s.so_ca} (${s.ngay_dat})</td>
                            <td>1</td>
                            <td><fmt:formatNumber value="${s.thanh_tien}" type="number"/></td>
                            <td style="font-weight:bold;color:#c62828;"><fmt:formatNumber value="${s.thanh_tien}" type="number"/></td>
                            <td>
                                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;">
                                    <input type="hidden" name="op" value="selectSlot">
                                    <input type="hidden" name="fieldId" value="${s.ma_san}">
                                    <input type="hidden" name="slot" value="${s.so_ca}">
                                    <input type="hidden" name="date" value="${s.ngay_dat}">
                                    <button type="submit" style="color:red;border:none;background:none;cursor:pointer;font-size:16px;">&times;</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:forEach items="${invoice.services}" var="sv">
                        <tr>
                            <td>${sv.ten_dv}</td>
                            <td>${sv.so_luong}</td>
                            <td><fmt:formatNumber value="${sv.don_gia}" type="number"/></td>
                            <td><fmt:formatNumber value="${sv.thanh_tien}" type="number"/></td>
                            <td>
                                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;">
                                    <input type="hidden" name="op" value="removeService">
                                    <input type="hidden" name="serviceId" value="${sv.ma_dv}">
                                    <input type="hidden" name="date" value="${viewDate}">
                                    <button type="submit" style="color:red;border:none;background:none;cursor:pointer;font-size:16px;">&times;</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <div class="invoice-total">
                <c:if test="${invoice.discount_amount > 0}">
                    <p>Tiền trước giảm: <fmt:formatNumber value="${invoice.raw_total}" type="number"/> VNĐ</p>
                    <p style="color:green;">Giảm giá: -<fmt:formatNumber value="${invoice.discount_amount}" type="number"/> VNĐ</p>
                </c:if>
                <p>Tổng cộng: <b><fmt:formatNumber value="${invoice.tong_tien}" type="number"/> VNĐ</b></p>
                <p>Tiền cọc (30%): <fmt:formatNumber value="${invoice.tien_coc}" type="number"/> VNĐ</p>
                <p style="color:#c62828;">Còn lại: <b><fmt:formatNumber value="${invoice.so_tien_con_lai}" type="number"/> VNĐ</b></p>
            </div>

            <div style="margin-top: 15px; border-top: 1px solid #eee; padding-top: 10px;">
                <b>Áp dụng Voucher (Sân):</b>
                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin-top:5px;">
                    <input type="hidden" name="op" value="applyVoucher">
                    <input type="hidden" name="date" value="${viewDate}">
                    <select name="voucherId" onchange="this.form.submit()" style="padding:5px; width: 100%;">
                        <option value="">-- Chọn Voucher Sân --</option>
                        <c:forEach items="${vouchers}" var="v">
                            <%-- Chỉ render voucher có loai = 1 (hoặc '1') --%>
                            <c:if test="${v.loai == 1 || v.loai == '1'}">
                                <option value="${v.ma_voucher}" ${invoice.ma_voucher == v.ma_voucher ? 'selected' : ''}>
                                        ${not empty v.ma_code ? v.ma_code : v.ma_voucher}
                                    <c:if test="${not empty v.ten_voucher}"> — ${v.ten_voucher}</c:if>
                                    — Giảm <fmt:formatNumber value="${v.gia_tri}" type="number"/>
                                        ${(v.loai_giam eq 'PERCENT' or v.loai_giam eq 'GIAM_THEO_PHAN_TRAM' or v.loai_giam eq '1') ? '%' : ' VNĐ'}
                                </option>
                            </c:if>
                        </c:forEach>
                    </select>
                </form>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</div>
</div>
</c:otherwise>
</c:choose>
</div>
<script>
(function(){
    var dateInput = document.getElementById('booking-date');
    if (dateInput) {
        dateInput.addEventListener('change', function(){
            window.location.href = '${pageContext.request.contextPath}/booking?date=' + this.value;
        });
    }
})();
</script>
```

### 8.2 home.jsp (Trang chủ)
**Đường dẫn:** `/jsp/user/home.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
    body {
        background-repeat: no-repeat;      /* Không lặp */
        background-size: cover;            /* Phủ kín */
        background-position: center;       /* Căn giữa */
        background-attachment: fixed;      /* Cuộn nền cố định */
    }
.hero-section {
    background: linear-gradient(135deg, #1565c0 0%, #0d47a1 100%);
    color: white;
    padding: 60px 20px;
    text-align: center;
    border-radius: 0 0 30px 30px;
    margin-bottom: 40px;
    box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}
.hero-section h1 { font-size: 2.5em; margin-bottom: 10px; }
.hero-section p { font-size: 1.2em; opacity: 0.9; }

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 20px;
    margin-top: -30px;
    padding: 0 20px;
}
.stat-card {
    background: white;
    padding: 20px;
    border-radius: 12px;
    text-align: center;
    box-shadow: 0 4px 10px rgba(0,0,0,0.05);
    transition: transform 0.3s;
}
.stat-card:hover { transform: translateY(-5px); }
.stat-card i { font-size: 24px; display: block; margin-bottom: 10px; }
.stat-card .value { font-size: 20px; font-weight: bold; display: block; }
.stat-card .label { color: #666; font-size: 14px; }

.fields-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 25px;
    padding: 20px;
}
.field-card {
    background: white;
    border-radius: 15px;
    overflow: hidden;
    box-shadow: 0 5px 15px rgba(0,0,0,0.08);
    transition: all 0.3s ease;
    border: 1px solid #eee;
}
.field-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 25px rgba(0,0,0,0.15);
}
.field-card-img {
    height: 150px;
    background: #e3f2fd;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 50px;
}
.field-card-content { padding: 20px;}
.field-card-content h3 { margin: 0 0 10px 0; color: #1565c0; }
.field-card-content p { color: #666; margin: 5px 0; font-size: 14px; }
.field-price { font-size: 18px; color: #d32f2f; font-weight: bold; margin: 15px 0; }
.field-card-btn {
    display: block;
    width: 95%;
    margin: 0 auto;
    text-align: center;
    background: #1565c0;
    color: white;
    padding: 12px;
    text-decoration: none;
    border-radius: 8px;
    font-weight: bold;
    transition: background 0.2s;
}
.field-card-btn:hover { background: #0d47a1; }
</style>
<body>
<div class="hero-section">
    <h1>⚽ Sân Bóng Hoa Sen</h1>
    <p>Hệ thống đặt sân hiện đại - Uy tín - Chất lượng hàng đầu</p>
    <div style="margin-top:25px;">
        <a href="${pageContext.request.contextPath}/booking" class="btn" style="background:#ffca28;color:#333;font-weight:bold;padding:12px 30px;border-radius:25px;text-decoration:none;">ĐẶT LỊCH NGAY</a>
    </div>
</div>

<div class="container">
    <div class="stats-grid">
        <div class="stat-card">
            <span style="font-size:30px;">✅</span>
            <span class="value">${stats.free}</span>
            <span class="label">Sân trống</span>
        </div>
        <div class="stat-card">
            <span style="font-size:30px;">⏳</span>
            <span class="value">${stats.pending}</span>
            <span class="label">Đang chờ</span>
        </div>
        <div class="stat-card">
            <span style="font-size:30px;">🏃</span>
            <span class="value">${stats.booked}</span>
            <span class="label">Đã đặt</span>
        </div>
        <div class="stat-card">
            <span style="font-size:30px;">⛔</span>
            <span class="value">${stats.expired}</span>
            <span class="label">Hết</span>
        </div>
    </div>

</div>
</body>
<%@ include file="/jsp/common/footer.jspf" %>
```

### 8.3 field-dashboard.jsp (Quản lý sân)
**Đường dẫn:** `/jsp/admin/field-dashboard.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.field-layout{display:flex;gap:20px;align-items:flex-start;}
.field-sidebar{flex:0 0 200px;}
.field-list{width:100%;}
.field-item{display:block;padding:12px 15px;margin:5px 0;background:#fff;border:1px solid #ccc;border-radius:4px;text-decoration:none;color:#000;cursor:pointer;transition:all .2s;}
.field-item:hover{background:#e3f2fd;border-color:#2196f3;box-shadow:0 2px 8px rgba(33,150,243,.15);}
.field-item.active{background:#1565c0;color:#fff;border-color:#1565c0;box-shadow:0 2px 8px rgba(21,101,192,.3);font-weight:bold;}
.field-item .field-badge{display:inline-block;padding:2px 8px;border-radius:10px;font-size:11px;margin-top:4px;background:#e0e0e0;}
.field-item.active .field-badge{background:rgba(255,255,255,.2);color:#fff;}
.customer-table{width:100%;border-collapse:collapse;}
.customer-table th,.customer-table td{border:1px solid #ccc;padding:8px;text-align:center;}
.customer-table th{background:#f5f5f5;white-space:nowrap;}
.customer-table tr:hover{background:#f0f7ff;}
.status-badge{display:inline-block;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:bold;color:#fff;}
.status-badge.hold{background:#ff9800;}
.status-badge.booked{background:#f44336;}
.status-badge.free{background:#4caf50;}
.status-badge.expired{background:#9e9e9e;}
.status-badge.event{background:#9c27b0;}
.slot-chip{display:inline-block;padding:3px 10px;border-radius:12px;background:#e8f5e9;color:#2e7d32;font-size:12px;font-weight:bold;white-space:nowrap;}
.session-label{font-weight:bold;color:#1565c0;}

/* Nổi bật cột Sân - toàn bộ ô có nền xanh gradient */
td.field-col{background:linear-gradient(135deg,#1565c0,#1976d2) !important;color:#fff;font-weight:bold;font-size:13px;letter-spacing:.3px;}
td.field-col span{color:#fff;}

/* Nổi bật cột Ca giờ - toàn bộ ô có nền cam gradient */
td.slot-col{background:linear-gradient(135deg,#e65100,#ef6c00) !important;color:#fff;font-weight:bold;}
td.slot-col .session-label,td.slot-col .slot-chip{color:#fff;background:rgba(255,255,255,.2);}
</style>
<div class="container">
<p><a class="btn" href="${pageContext.request.contextPath}/admin">&larr; Dashboard</a></p>
<h2>Quản lý Sân</h2>

<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>

<div class="field-layout">
<!-- Sidebar - Field List -->
<div class="field-sidebar">
<h3>Danh sách sân</h3>
<div class="field-list">
<c:forEach items="${fields}" var="f">
<a href="?field=${f.ma_san}" class="field-item ${f.ma_san == selectedField ? 'active' : ''}">
<strong>${f.ten_san}</strong>
<div style="font-size:12px;color:${f.ma_san == selectedField ? '#fff' : '#666'};margin-top:3px;">
<span class="field-badge">${f.loai_san}</span>
<c:if test="${not empty f.gia_theo_gio}">
<span style="margin-left:5px;"><fmt:formatNumber value="${f.gia_theo_gio}" type="number"/><small>đ/giờ</small></span>
</c:if>
</div>
</a>
</c:forEach>
</div>
</div>

<!-- Main Content - Customer Info Table -->
<div style="flex:1;">
<c:if test="${not empty selectedField}">
<h3>Khách hàng đã đặt sân ${selectedFieldName}</h3>
<c:if test="${empty bookings}">
<p>Chưa có lịch đặt nào cho sân này.</p>
</c:if>
<c:if test="${not empty bookings}">
<table class="customer-table">
<thead>
<tr>
<th>Mã đặt</th>
<th>Sân</th>
<th>Khách hàng</th>
<th>Số điện thoại</th>
<th>Ngày đặt</th>
<th>Ca</th>
<th>Trạng thái</th>
</tr>
</thead>
<tbody>
<c:forEach items="${bookings}" var="b">
<tr>
<td><strong>${b.ma_lich}</strong></td>
<td class="field-col">
<c:if test="${not empty b.ten_san}">
${b.ten_san}
</c:if>
</td>
<td>${b.ten_khach}</td>
<td>${b.sdt}</td>
<td>${b.ngay_dat}</td>
<td class="slot-col">
<c:set var="slotLabel" value="Ca ${b.so_ca}"/>
<c:if test="${not empty b.buoi}">
<span>${b.buoi}</span>&nbsp;
</c:if>
<c:if test="${not empty b.ca}">
<span>${b.ca}</span>
</c:if>
</td>
<td>
<c:choose>
<c:when test="${b.status_code == 1}"><span class="status-badge hold">Đang giữ</span></c:when>
<c:when test="${b.status_code == 2}"><span class="status-badge booked">Đã đặt</span></c:when>
<c:when test="${b.status_code == 3}"><span class="status-badge expired">Hết</span></c:when>
<c:when test="${b.status_code == 4}"><span class="status-badge event">Sự kiện</span></c:when>
<c:otherwise><span class="status-badge free">Trống</span></c:otherwise>
</c:choose>
</td>
</tr>
</c:forEach>
</tbody>
</table>

<h3 style="margin-top:30px;">Hóa đơn của sân ${selectedFieldName}</h3>
<c:if test="${empty invoices}">
<p>Chưa có hóa đơn nào.</p>
</c:if>
<c:if test="${not empty invoices}">
<table class="customer-table">
<thead>
<tr>
<th>Mã HD</th>
<th>Khách hàng</th>
<th>Tổng tiền</th>
<th>Ngày xuất</th>
<th>Trạng thái</th>
</tr>
</thead>
<tbody>
<c:forEach items="${invoices}" var="i">
<tr>
<td>${i.ma_hd}</td>
<td>${i.ten_khach}</td>
<td><fmt:formatNumber value="${i.tong_tien}" type="number"/>đ</td>
<td>${i.ngay_xuat}</td>
<td>
<span style="color:${i.da_thu ? '#4caf50' : '#f44336'}">
${i.da_thu ? 'Đã thu' : 'Chưa thu'}
</span>
</td>
</tr>
</c:forEach>
</tbody>
</table>
</c:if>
</c:if>
</c:if>

<c:if test="${empty selectedField}">
<p style="color:#666;">Vui lòng chọn sân từ danh sách bên trái để xem thông tin khách hàng.</p>
</c:if>
</div>
</div>
</div>
<%@ include file="/jsp/common/footer.jspf" %>
```

---

## 9. CÁC CLASS DÙNG CHUNG

### 9.1 DBConnection.java
**Đường dẫn:** `src/main/java/com/footballbooking/config/DBConnection.java`

**Được sử dụng bởi:**
- Người 1 (AdminLoginServlet, AdminCrudServlet)
- Người 2 (BookingServlet, BookingRepository)
- Người 3 (VoucherService, VoucherRepository)
- Người 4 (AdminInvoiceApiServlet, FieldDashboardServlet)

**Code hoàn chỉnh:**
```java
package com.footballbooking.config;

import java.sql.*;

/** 
 * Cấu hình kết nối SQL Server. 
 */
public final class DBConnection {
    private static volatile boolean driverLoaded = false;

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            System.err.println("CẢNH BÁO: Không tìm thấy driver mssql-jdbc. " + e.getMessage());
        }
    }

    private DBConnection() {}
    
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:sqlserver://localhost:1433;databaseName=QuanLySanBong;encrypt=true;trustServerCertificate=true");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "sa");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "123456");

    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            throw new SQLException("Driver SQL Server JDBC không khả dụng.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

### 9.2 Entities.java
**Đường dẫn:** `src/main/java/com/footballbooking/model/Entities.java`

**Entity liên quan đến Người 2:**
- `San` - Sân bóng
- `LichSan` - Lịch sân
- `DichVu` - Dịch vụ
- `KhachHang` - Khách hàng

**Code hoàn chỉnh:** (Xem chi tiết trong file 01_NguoiDung_DangNhap.md)

### 9.3 Slots.java
**Đường dẫn:** `src/main/java/com/footballbooking/utils/Slots.java`

**Lý do cần thiết:** Định nghĩa cấu trúc 9 ca sân trong ngày.

**Code hoàn chỉnh:**
```java
package com.footballbooking.utils;

/**
 * Lớp định nghĩa cấu trúc các ca đặt sân trong ngày.
 * Hiện tại hỗ trợ 9 ca cố định, mỗi ca kéo dài 2 giờ.
 */
public final class Slots {
    private Slots() {}

    /** 
     * Dữ liệu các ca: [Buổi, Số ca, Giờ bắt đầu, Giờ kết thúc, Nhãn hiển thị]
     */
    public static final String[][] DATA = {
            {"Sáng", "1", "06:00", "08:00", "06:00-08:00"},
            {"Sáng", "2", "08:00", "10:00", "08:00-10:00"},
            {"Sáng", "3", "10:00", "12:00", "10:00-12:00"},
            {"Chiều", "4", "12:00", "14:00", "12:00-14:00"},
            {"Chiều", "5", "14:00", "16:00", "14:00-16:00"},
            {"Chiều", "6", "16:00", "18:00", "16:00-18:00"},
            {"Tối", "7", "18:00", "20:00", "18:00-20:00"},
            {"Tối", "8", "20:00", "22:00", "20:00-22:00"},
            {"Tối", "9", "22:00", "00:00", "22:00-00:00"}
    };

    /**
     * Lấy thông tin chi tiết của một ca dựa trên số thứ tự ca.
     */
    public static String[] get(int ca) {
        for (String[] s : DATA) {
            if (Integer.parseInt(s[1]) == ca) return s;
        }
        throw new IllegalArgumentException("Ca không hợp lệ: " + ca);
    }

    /**
     * Kiểm tra xem số ca cung cấp có nằm trong phạm vi hợp lệ hay không.
     */
    public static boolean valid(int ca) {
        return ca >= 1 && ca <= 9;
    }
}
```

---

## 10. LUỒNG XỬ LÝ

### 10.1 Luồng đặt sân

```
Người dùng truy cập /booking
        ↓
BookingServlet.doGet()
        ↓
Tải danh sách sân, lịch sân, dịch vụ
        ↓
Hiển thị booking.jsp
        ↓
Người dùng chọn sân/ca
        ↓
Gửi POST /booking?op=selectSlot
        ↓
BookingService.ensureInvoice() - Tạo hóa đơn nháp
        ↓
BookingService.toggleSlot() - Thêm sân vào hóa đơn
        ↓
Cập nhật Session (activeInvoiceId)
        ↓
Người dùng thêm dịch vụ (tương tự)
        ↓
Người dùng xác nhận đặt sân
        ↓
Gửi POST /booking?op=confirm
        ↓
Chuyển đến /invoice?id=xxx
```

### 10.2 Luồng kiểm tra trạng thái sân

```
Truy cập /booking?date=2026-07-04
        ↓
BookingService.fullSchedule()
        ↓
BookingRepository.slots()
        ↓
SELECT ... FROM LichSan WHERE ma_san=? AND ngay_dat=? AND so_ca=?
        ↓
Đếm trạng thái cao nhất (BOOKED > EVENT > HOLD > EXPIRED > FREE)
        ↓
Nếu status_code=0 và giờ bắt đầu <= hiện tại → EXPIRED
        ↓
Trả về danh sách 9 ca với trạng thái
```

### 10.3 Luồng thanh toán

```
Người dùng xác nhận đặt sân
        ↓
Chuyển đến /invoice
        ↓
InvoiceServlet hiển thị chi tiết
        ↓
Người dùng chọn "Thanh toán cọc"
        ↓
Gửi POST /invoice?op=pay
        ↓
BookingService.payDeposit()
        ↓
BookingRepository.pay()
        ↓
Cập nhật HoaDon: UNPAID → MỚI THANH TOÁN CỌC
        ↓
Cập nhật LichSan: ĐANG GIỮ CHỖ → ĐÃ ĐẶT
        ↓
Giảm số lượng voucher
```

---

## 11. CÁC LỖI THƯỜNG GẶP

### 11.1 Lỗi sân không hiển thị

**Triệu chứng:** Danh sách sân trống hoặc không load

**Nguyên nhân:**
- Database chưa có dữ liệu sân
- Lỗi kết nối DB
- Trạng thái sân không phải 'HOẠT ĐỘNG'

**Cách sửa:**
```sql
-- Kiểm tra sân
SELECT * FROM San WHERE trang_thai=N'HOẠT ĐỘNG';

-- Thêm sân mẫu nếu chưa có
INSERT INTO San (ma_san, ten_san, loai_san, gia_theo_gio, trang_thai) VALUES
('S01', N'Sân 1', N'Sân 7 người', 600000, N'HOẠT ĐỘNG');
```

### 11.2 Lỗi không đặt được sân

**Triệu chứng:** Click chọn sân nhưng không thêm vào hóa đơn

**Nguyên nhân:**
- Ca đã qua giờ bắt đầu
- Sân đã được đặt
- Hóa đơn bị xóa

**Cách sửa:**
```java
// Kiểm tra trong BookingRepository.isSlotFree()
// Đảm bảo ca chưa qua giờ và sân còn trống
```

### 11.3 Lỗi giờ kết thúc âm

**Triệu chứng:** Ca 22:00-00:00 hiển thị sai

**Nguyên nhân:**
- Xử lý ngày tháng chưa đúng
- Ca kéo dài sang ngày hôm sau

**Cách sửa:**
```java
// Trong BookingRepository.insertLichSanRecord()
if (!end.isAfter(start)) endDt = endDt.plusDays(1);
```

### 11.4 Lỗi Session hóa đơn bị mất

**Triệu chứng:** Đang chọn sân mà hóa đơn bị xóa

**Nguyên nhân:**
- Hóa đơn trống bị xóa tự động
- Session timeout

**Cách sửa:**
```java
// Trong BookingServlet.doGet()
// Kiểm tra hóa đơn còn tồn tại không
if (inv != null) {
    // tiếp tục
} else {
    session.removeAttribute("activeInvoiceId");
}
```

---

## 12. LƯU Ý QUAN TRỌNG

### 12.1 CLASS DÙNG CHUNG

Các class sau là CLASS DÙNG CHUNG:

1. **BookingRepository.java** (1189 dòng)
   - Được sử dụng bởi tất cả các thành viên
   - Người 2 cần đọc hiểu toàn bộ để biết cách đặt sân
   - Không sửa code liên quan đến Voucher, Hóa đơn của người khác

2. **BookingService.java** (139 dòng)
   - Lớp trung gian giữa Controller và Repository
   - Người 2 phụ trách toàn bộ

3. **DBConnection.java** (41 dòng)
   - Tất cả đều dùng
   - Không sửa

4. **Entities.java** (196 dòng)
   - Chỉ quan tâm San, LichSan, DichVu, KhachHang
   - Không sửa các Entity khác

### 12.2 QUY TẮC CODE

1. **Luôn kiểm tra trạng thái sân trước khi đặt:**
```java
// Trước khi đặt, phải kiểm tra:
if (!isSlotFree(fieldId, date, slot)) {
    throw new IllegalStateException("Sân đã được đặt");
}
```

2. **Xử lý transaction đúng cách:**
```java
c.setAutoCommit(false);
try {
    // Thao tác DB
    c.commit();
} catch (Exception e) {
    c.rollback();
    throw e;
} finally {
    c.setAutoCommit(true);
}
```

3. **Tạo mã tự động:**
```java
// Sử dụng hàm next() trong Repository
String maLich = next(c, "LichSan", "ma_lich", "L", 3);
// Kết quả: L001, L002, ...
```

4. **Không được xóa sân đã đặt:**
```sql
-- Chỉ được xóa sân có trạng thái CÒN TRỐNG hoặc ĐÃ HỦY
-- Sân ĐÃ ĐẶT phải hủy hóa đơn trước
```

---

## 13. TÀI LIỆU THAM KHẢO

- **Java Servlet:** https://docs.oracle.com/javaee/8/tutorial/servlets.htm
- **JDBC:** https://docs.oracle.com/javase/tutorial/jdbc/
- **SQL Server:** https://docs.microsoft.com/en-us/sql/

---

**NGƯỜI PHỤ TRÁCH:** Người 2  
**FILE ĐẦU RA:** 02_QuanLySan_DatSan.md  
**TRẠNG THÁI:** Hoàn thành