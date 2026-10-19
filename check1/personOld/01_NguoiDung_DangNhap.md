# 01_NGUOI DUNG_DANG NHAP.md
## PHẦN 1 — NGƯỜI 1: QUẢN LÝ NGƯỜI DÙNG + ĐĂNG NHẬP

---

## 1. CHỨC NĂNG PHỤ TRÁCH

Người 1 phụ trách toàn bộ chức năng liên quan đến người dùng và xác thực:

- Đăng nhập (Login)
- Đăng xuất (Logout)
- Quản lý thông tin khách hàng (CRUD)
- Quản lý thông tin nhân viên (CRUD)
- Phân quyền (Admin/Staff)
- Quản lý Session

**Lưu ý quan trọng:**
- Không code chức năng đặt sân, voucher, hóa đơn, thanh toán, hay CronJob
- Chỉ làm việc trên các file liên quan đến người dùng và xác thực
- Nếu cần sửa file của người khác, phải đánh dấu "FILE DÙNG CHUNG"

---

## 2. CẤU TRÚC THƯ MỤC

```
src/main/java/com/footballbooking/
├── config/
│   ├── DBConnection.java                [CLASS DÙNG CHUNG]
│   └── ExpiryListener.java              [CLASS DÙNG CHUNG]
├── controller/
│   ├── AdminLoginServlet.java           (Đăng nhập Admin)
│   ├── AdminLogoutServlet.java          (Đăng xuất Admin)
│   ├── AdminCrudServlet.java            [CLASS DÙNG CHUNG] - CRUD chung
│   ├── BookingServlet.java              [CLASS DÙNG CHUNG]
│   ├── InvoiceServlet.java              [CLASS DÙNG CHUNG]
│   ├── ScheduleApiServlet.java          [CLASS DÙNG CHUNG]
│   └── ... (các Servlet khác do người khác phụ trách)
├── filter/
│   └── AdminAuthFilter.java             (Bộ lọc kiểm tra đăng nhập Admin)
├── model/
│   └── Entities.java                    [CLASS DÙNG CHUNG]
├── repository/
│   ├── BookingRepository.java           [CLASS DÙNG CHUNG]
│   └── VoucherRepository.java           [CLASS DÙNG CHUNG]
├── service/
│   ├── BookingService.java              [CLASS DÙNG CHUNG]
│   └── VoucherService.java              [CLASS DÙNG CHUNG]
└── utils/
    ├── DbConnection.java                [CLASS DÙNG CHUNG]
    ├── JsonUtil.java                    [CLASS DÙNG CHUNG]
    └── Slots.java                       [CLASS DÙNG CHUNG]

src/main/webapp/jsp/
├── admin/
│   ├── login.jsp                       (Trang đăng nhập Admin)
│   ├── dashboard.jsp                   (Trang chủ Admin)
│   ├── form.jsp                        (Form thêm/sửa)
│   └── list.jsp                        (Danh sách)
├── common/
│   ├── header.jspf                     (Header chung)
│   └── footer.jspf                     (Footer chung)
└── user/
    ├── home.jsp                        [CLASS DÙNG CHUNG]
    ├── booking.jsp                     [CLASS DÙNG CHUNG]
    └── invoice.jsp                     [CLASS DÙNG CHUNG]

database/
└── football_booking.sql                [CLASS DÙNG CHUNG]
```

---

## 3. DATABASE LIÊN QUAN

### 3.1 Bảng NhanVien (Nhân viên - Admin)
```sql
CREATE TABLE NhanVien (
    ma_nv    VARCHAR(10) PRIMARY KEY,
    ten_nv   NVARCHAR(100) NOT NULL,
    sdt      VARCHAR(15) NOT NULL UNIQUE,
    chuc_vu  NVARCHAR(50) NOT NULL,
    mat_khau NVARCHAR(100) NOT NULL DEFAULT N'admin123'
);
```

**Dữ liệu mẫu:**
```sql
INSERT INTO NhanVien (ma_nv, ten_nv, sdt, chuc_vu, mat_khau) VALUES
('NV001', N'Quản trị viên', '0900000000', N'Admin', N'admin123'),
('NV002', N'Quản trị viên 2', '0365365113', N'Admin', N'1');
```

### 3.2 Bảng KhachHang (Khách hàng)
```sql
CREATE TABLE KhachHang (
    ma_kh   VARCHAR(10) PRIMARY KEY,
    ho_ten  NVARCHAR(100) NOT NULL,
    sdt     VARCHAR(15) NOT NULL UNIQUE,
    email   VARCHAR(100) NULL
);
```

**Lưu ý:**
- Khách hàng được tạo tự động khi đặt sân (qua BookingService)
- Không có mật khẩu, chỉ có thông tin liên hệ

---

## 4. CÁC ENTITY

### 4.1 Entity NhanVien
```java
public static class NhanVien {
    public String maNv, tenNv, sdt, chucVu, matKhau;
}
```

**Giải thích:**
- `maNv`: Mã nhân viên (VD: NV001)
- `tenNv`: Tên nhân viên
- `sdt`: Số điện thoại (dùng để đăng nhập)
- `chucVu`: Chức vụ (Admin, Staff...)
- `matKhau`: Mật khẩu (đã mã hóa)

### 4.2 Entity KhachHang
```java
public static class KhachHang {
    public String maKh, hoTen, sdt, email;
}
```

**Giải thích:**
- `maKh`: Mã khách hàng (VD: KH001)
- `hoTen`: Họ tên khách hàng
- `sdt`: Số điện thoại
- `email`: Email (tùy chọn)

---

## 5. CÁC SERVLET

### 5.1 AdminLoginServlet.java
**Đường dẫn:** `/admin/login`
**Chức năng:** Xử lý đăng nhập cho Admin/Nhân viên

**Code hoàn chỉnh:**
```java
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
```

**Giải thích luồng xử lý:**
1. Người dùng truy cập `/admin/login`
2. Kiểm tra Session, nếu đã đăng nhập → chuyển đến Dashboard
3. Thực hiện tự động đăng nhập với tài khoản demo
4. Gọi `authenticate()` để kiểm tra trong DB
5. Nếu đúng → lưu vào Session và chuyển đến Dashboard
6. Nếu sai → dùng tài khoản mặc định

### 5.2 AdminLogoutServlet.java
**Đường dẫn:** `/admin/logout`
**Chức năng:** Đăng xuất Admin

**Code hoàn chỉnh:**
```java
package com.footballbooking.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * Servlet xử lý đăng xuất cho quản trị viên.
 * Xóa thông tin Session và chuyển hướng về trang đăng nhập.
 */
@WebServlet("/admin/logout")
public class AdminLogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Hủy bỏ session hiện tại nếu có
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // Chuyển hướng về trang đăng nhập
        resp.sendRedirect(req.getContextPath() + "/admin/login");
    }
}
```

### 5.3 AdminAuthFilter.java
**Đường dẫn:** `/admin/*` (Filter)
**Chức năng:** Kiểm tra quyền truy cập khu vực Admin

**Code hoàn chỉnh:**
```java
package com.footballbooking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Filter bảo vệ các tài nguyên trong khu vực quản trị (/admin/*).
 * Kiểm tra quyền truy cập của người dùng trước khi cho phép vào các trang quản lý.
 */
@WebFilter(urlPatterns = {"/admin/*"})
public class AdminAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Cho phép truy cập không cần đăng nhập vào trang login và logout
        String servletPath = req.getServletPath();
        if ("/admin/login".equals(servletPath) || "/admin/logout".equals(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Kiểm tra session xem admin đã đăng nhập chưa
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("adminUser") == null) {
            // Nếu chưa đăng nhập, chuyển hướng về trang đăng nhập
            resp.sendRedirect(req.getContextPath() + "/admin/login");
            return;
        }
        
        // Đã đăng nhập, cho phép đi tiếp
        chain.doFilter(request, response);
    }
}
```

**Giải thích luồng phân quyền:**
1. Người dùng truy cập `/admin/*`
2. Filter kiểm tra Session có `adminUser` không
3. Nếu không có → chuyển về `/admin/login`
4. Nếu có → cho phép truy cập

### 5.4 AdminCrudServlet.java (Phần quản lý người dùng)
**Đường dẫn:** `/admin/manage?entity=KhachHang` hoặc `/admin/manage?entity=NhanVien`
**Chức năng:** CRUD Khách hàng và Nhân viên

**Lưu ý quan trọng:** Đây là CLASS DÙNG CHUNG. Người 1 chỉ quan tâm phần xử lý `KhachHang` và `NhanVien`.

**Code hoàn chỉnh (959 dòng):**
```java
package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import com.footballbooking.model.Entities.Voucher;
import com.footballbooking.model.Entities.VoucherLoai;
import com.footballbooking.repository.BookingRepository;
import com.footballbooking.service.VoucherService;
import com.footballbooking.utils.Slots;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Servlet xử lý các thao tác CRUD (Thêm, Sửa, Xóa, Liệt kê) cho các thực thể trong hệ thống quản trị.
 * Hỗ trợ các bảng: Sân, Lịch Sân, Khách Hàng, Nhân Viên, Dịch Vụ, Hóa Đơn và Chi Tiết Hóa Đơn.
 */
@WebServlet("/admin/manage")
public class AdminCrudServlet extends HttpServlet {
    // Bản đồ chứa tên hiển thị tiếng Việt cho các bảng và cột trong CSDL
    private static final Map<String, String> L = new HashMap<>();
    static {
        L.put("San", "Sân"); L.put("LichSan", "Lịch Sân"); L.put("KhachHang", "Khách Hàng");
        L.put("NhanVien", "Nhân Viên"); L.put("DichVu", "Dịch Vụ"); L.put("HoaDon", "Hóa Đơn");
        L.put("CTHD", "Chi Tiết Hóa Đơn");
        L.put("ma_san", "Mã Sân"); L.put("ten_san", "Tên Sân"); L.put("loai_san", "Loại Sân");
        L.put("gia_theo_gio", "Giá/Giờ"); L.put("trang_thai", "Trạng Thái");
        L.put("ma_lich", "Mã Lịch"); L.put("ngay_dat", "Ngày Đặt"); L.put("buoi", "Buổi");
        L.put("so_ca", "Số Ca"); L.put("ca", "Ca"); L.put("gio_bat_dau", "Bắt Đầu");
        L.put("gio_ket_thuc", "Kết Thúc");
        L.put("ma_kh", "Mã KH"); L.put("ho_ten", "Họ Tên"); L.put("sdt", "Số Điện Thoại");
        L.put("email", "Email");
        L.put("ma_nv", "Mã NV"); L.put("ten_nv", "Tên NV"); L.put("chuc_vu", "Chức Vụ");
        L.put("ma_dv", "Mã DV"); L.put("ten_dv", "Tên DV"); L.put("don_gia", "Đơn Giá");
        L.put("ma_hd", "Mã HĐ"); L.put("ngay_lap", "Ngày Lập"); L.put("tong_tien", "Tổng Tiền");
        L.put("tien_coc", "Tiền Cọc"); L.put("so_tien_con_lai", "Còn Lại");
        L.put("han_thanh_toan", "Hạn Thanh Toán");
        L.put("ma_cthd", "Mã CTHD"); L.put("so_luong", "Số Lượng"); L.put("thanh_tien", "Thành Tiền");
        L.put("Voucher", "Voucher");
        L.put("ma_voucher", "Mã Voucher");
        L.put("ma_code", "Mã Code");
        L.put("ten_voucher", "Tên Voucher");
        L.put("loai_giam", "Loại Giảm");
        L.put("gia_tri", "Giá Trị");
        L.put("don_toi_thieu", "Đơn Tối Thiểu");
        L.put("ngay_bat_dau", "Ngày Bắt Đầu");
        L.put("ngay_ket_thuc", "Ngày Kết Thúc");
        L.put("so_luong", "Số Lượng");
        L.put("trang_thai", "Trạng Thái");
        L.put("loai", "Loại Áp Dụng");
        // Alias legacy (nếu form cũ còn tham chiếu)
        L.put("idVoucher", "Mã Voucher");
        L.put("ngayBatDau", "Ngày Bắt Đầu");
        L.put("ngayKetThuc", "Ngày Kết Thúc");
        L.put("soLuong", "Số Lượng");
        L.put("loaiGiam", "Loại Giảm");
        L.put("mucToiThieu", "Mức Tối Thiểu");
        L.put("mucGiam", "Mức Giảm");
        L.put("loaiVoucher", "Loại Voucher");
    }

    // Tập hợp các trạng thái hợp lệ để kiểm tra tính toàn vẹn dữ liệu
    private static final Set<String> INVOICE_STATUS = new HashSet<>(Arrays.asList(
            "ĐÃ ĐẶT", "CHƯA THANH TOÁN", "MỚI THANH TOÁN CỌC", "ĐÃ THANH TOÁN", "ĐÃ HỦY", "ĐÃ XÓA"
    ));
    private static final Set<String> LICH_SAN_STATUS = new HashSet<>(Arrays.asList(
            "CÒN TRỐNG", "ĐANG GIỮ CHỖ", "ĐÃ ĐẶT", "SỰ KIỆN", "HẾT"
    ));
    private static final Set<String> SAN_STATUS = new HashSet<>(Arrays.asList(
            "HOẠT ĐỘNG", "BẢO TRÌ", "ĐÃ XÓA"
    ));

    /**
     * Lớp nội bộ lưu trữ thông tin cấu trúc (Metadata) của một bảng.
     */
    public static final class Meta {
        final String table, pk;
        final String[] cols;
        Meta(String t, String p, String... c) {
            table = t; pk = p; cols = c;
        }
        public String getTable() { return table; }
        public String getPk() { return pk; }
        public String[] getCols() { return cols; }
    }

    // Định nghĩa Metadata cho từng bảng để xử lý tự động trong Servlet
    private static final Map<String, Meta> M = new LinkedHashMap<>();
    static {
        M.put("San", new Meta("San", "ma_san", "ma_san", "ten_san", "loai_san", "gia_theo_gio", "trang_thai"));
        M.put("LichSan", new Meta("LichSan", "ma_lich", "ma_lich", "ma_san", "ngay_dat", "buoi", "so_ca", "ca", "gio_bat_dau", "gio_ket_thuc", "trang_thai"));
        M.put("KhachHang", new Meta("KhachHang", "ma_kh", "ma_kh", "ho_ten", "sdt", "email"));
        M.put("NhanVien", new Meta("NhanVien", "ma_nv", "ma_nv", "ten_nv", "sdt", "chuc_vu"));
        M.put("DichVu", new Meta("DichVu", "ma_dv", "ma_dv", "ten_dv", "don_gia"));
        M.put("HoaDon", new Meta("HoaDon", "ma_hd", "ma_hd", "ma_kh", "ma_nv", "ngay_lap", "tong_tien", "tien_coc", "so_tien_con_lai", "han_thanh_toan", "trang_thai"));
        M.put("CTHD", new Meta("CTHD", "ma_cthd", "ma_cthd", "ma_hd", "ma_lich", "ma_dv", "so_luong", "don_gia", "thanh_tien"));
        M.put("Voucher", new Meta("Voucher", "ma_voucher",
                "ma_voucher", "ma_code", "ten_voucher", "loai_giam", "gia_tri",
                "don_toi_thieu", "ngay_bat_dau", "ngay_ket_thuc", "so_luong", "trang_thai", "loai"));
    }

    private final VoucherService voucherService = new VoucherService();

    private Meta meta(HttpServletRequest q) throws ServletException {
        Meta m = M.get(q.getParameter("entity"));
        if (m == null) throw new ServletException("Danh mục không hợp lệ");
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            q.setAttribute("L", L);
            Meta m = meta(q);
            String a = q.getParameter("action");

            // Nếu action là 'form', hiển thị giao diện Thêm/Sửa
            if ("form".equals(a)) {
                prepareForm(q, m);
                q.getRequestDispatcher("/jsp/admin/form.jsp").forward(q, p);
                return;
            }
            // Mặc định liệt kê danh sách bản ghi
            list(q, p, m);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Chuẩn bị dữ liệu cho form Thêm/Sửa.
     */
    private void prepareForm(HttpServletRequest q, Meta m) throws Exception {
        String id = q.getParameter("id");
        Map<String, Object> record = find(m, id);
        if (record == null && "Voucher".equals(m.table)) {
            record = new LinkedHashMap<>();
            // Prefill loai khi thêm từ trang Sân / Dịch vụ
            String loaiParam = q.getParameter("loai");
            if (loaiParam != null && !loaiParam.isBlank()) {
                record.put("loai", Integer.parseInt(loaiParam.trim()));
            } else {
                record.put("loai", VoucherLoai.HOA_DON);
            }
            record.put("trang_thai", "HOẠT ĐỘNG");
            record.put("loai_giam", "MONEY");
        }
        q.setAttribute("record", record);
        q.setAttribute("isNew", id == null);
        q.setAttribute("meta", m);

        if ("HoaDon".equals(m.table)) {
            prepareHoaDonForm(q, id);
        } else if ("CTHD".equals(m.table) && id != null) {
            prepareCthdForm(q, record);
        }
    }

    /**
     * Chuẩn bị dữ liệu đặc thù cho form Hóa đơn.
     * Tối ưu hiệu năng: dùng EXISTS để xác định sân đã thuộc hóa đơn.
     * Đảm bảo tất cả sân còn trống trong cùng ngày đặt đều hiển thị.
     */
    private void prepareHoaDonForm(HttpServletRequest q, String id) throws SQLException {
        BookingRepository repo = new BookingRepository();
        q.setAttribute("services", repo.services());

        // Xác định ngày đặt của hóa đơn
        java.sql.Date invoiceDate = null;
        if (id != null) {
            Map<String, Object> inv = repo.invoice(id);
            if (inv != null) {
                q.setAttribute("inv", inv);
                Map<String, Integer> qty = new HashMap<>();
                List<String> sids = new ArrayList<>();
                List<Map<String, Object>> servicesList = (List<Map<String, Object>>) inv.get("services");
                for (Map<String, Object> s : servicesList) {
                    qty.put((String) s.get("ma_dv"), (Integer) s.get("so_luong"));
                    sids.add((String) s.get("ma_dv"));
                }
                q.setAttribute("editServiceQty", qty);
                q.setAttribute("editServices", sids);

                // Lấy ngày đặt từ slot đầu tiên của hóa đơn
                List<Map<String, Object>> invSlots = (List<Map<String, Object>>) inv.get("slots");
                if (invSlots != null && !invSlots.isEmpty()) {
                    Object ngayDat = invSlots.get(0).get("ngay_dat");
                    if (ngayDat instanceof java.sql.Date) {
                        invoiceDate = (java.sql.Date) ngayDat;
                    } else if (ngayDat instanceof java.sql.Timestamp) {
                        invoiceDate = new java.sql.Date(((java.sql.Timestamp) ngayDat).getTime());
                    } else if (ngayDat != null) {
                        try {
                            invoiceDate = java.sql.Date.valueOf(String.valueOf(ngayDat));
                        } catch (Exception ignored) { }
                    }
                }
            }
        }

        // Nếu không có ngày đặt (hóa đơn mới), dùng ngày hôm nay
        if (invoiceDate == null) {
            invoiceDate = java.sql.Date.valueOf(LocalDate.now());
        }

        // Xây dựng bảng tạm chứa 9 ca trong ngày từ Slots.DATA (giống BookingRepository.slots)
        StringBuilder values = new StringBuilder();
        for (String[] x : Slots.DATA) {
            if (values.length() > 0) values.append(',');
            values.append('(').append(x[1]).append(",N'").append(x[0]).append("','")
                    .append(x[2]).append("','").append(x[3]).append("','").append(x[4]).append("')");
        }

        String sql =
                "SELECT " +
                        "s.ma_san, " +
                        "s.ten_san, " +
                        "ls.ma_lich, " +
                        "ls.ca, " +
                        "ls.ngay_dat, " +
                        "x.so_ca, " +
                        "x.gio_bat_dau, " +
                        "x.gio_ket_thuc, " +
                        "CASE WHEN c.ma_lich IS NOT NULL THEN 1 ELSE 0 END checked " +
                        "FROM (VALUES " + values + ") x(so_ca,buoi,gio_bat_dau,gio_ket_thuc,nhan_ca) " +
                        "CROSS JOIN San s " +
                        "LEFT JOIN LichSan ls ON ls.ma_san=s.ma_san AND ls.ngay_dat=? AND ls.so_ca=x.so_ca " +
                        "LEFT JOIN CTHD c ON c.ma_lich=ls.ma_lich AND c.ma_hd=? AND c.ma_dv IS NULL " +
                        "WHERE s.trang_thai=N'HOẠT ĐỘNG' " +
                        "AND ( " +
                        "      ls.ma_lich IS NULL " +
                        "      OR ls.trang_thai=N'CÒN TRỐNG' " +
                        "      OR c.ma_lich IS NOT NULL " +
                        ") " +
                        // Lọc ca đã hết hiệu lực: nếu là ngày hôm nay, chỉ hiển thị ca chưa qua giờ bắt đầu
                        // (trừ các ca đã thuộc hóa đơn này để người dùng vẫn có thể bỏ chọn)
                        "AND ( " +
                        "      ? <> CAST(SYSDATETIME() AS DATE) " +
                        "      OR CAST(x.gio_bat_dau AS TIME) > CAST(SYSDATETIME() AS TIME) " +
                        "      OR c.ma_lich IS NOT NULL " +
                        ") " +
                        "ORDER BY x.so_ca,s.ten_san";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, invoiceDate);
            s.setString(2, id);
            s.setDate(3, invoiceDate);
            List<Map<String, Object>> avail = new ArrayList<>();
            ResultSet r = s.executeQuery();
            while (r.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ma_san", r.getString("ma_san"));
                m.put("ma_lich", r.getString("ma_lich"));
                m.put("ten_san", r.getString("ten_san"));
                m.put("ca", r.getString("ca"));
                m.put("ngay_dat", r.getDate("ngay_dat"));
                m.put("so_ca", r.getInt("so_ca"));
                m.put("gio_bat_dau", r.getString("gio_bat_dau"));
                m.put("gio_ket_thuc", r.getString("gio_ket_thuc"));
                // Đánh dấu đã chọn nếu sân đã thuộc hóa đơn này
                m.put("checked", r.getInt("checked") == 1);
                avail.add(m);
            }
            q.setAttribute("availableSlots", avail);
        }
    }

    /**
     * Chuẩn bị dữ liệu đặc thù cho form Chi tiết hóa đơn.
     */
    private void prepareCthdForm(HttpServletRequest q, Map<String, Object> record) throws SQLException {
        if (record != null && record.get("ma_hd") != null) {
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement s = c.prepareStatement("SELECT trang_thai FROM HoaDon WHERE ma_hd=?")) {
                s.setString(1, String.valueOf(record.get("ma_hd")));
                ResultSet r = s.executeQuery();
                if (r.next()) q.setAttribute("invoiceStatus", r.getString(1));
            }
        }
    }

    /**
     * Liệt kê danh sách các bản ghi có phân trang, tìm kiếm và sắp xếp.
     */
    private void list(HttpServletRequest q, HttpServletResponse p, Meta m) throws Exception {
        String search = val(q, "search");
        String status = val(q, "status");
        String sort = val(q, "sort");
        if (!contains(m.cols, sort)) sort = m.pk;

        // Mặc định hóa đơn mới nhất lên đầu
        String dirParam = q.getParameter("dir");
        if (dirParam == null && "HoaDon".equals(m.table)) dirParam = "desc";
        String dir = "desc".equalsIgnoreCase(dirParam) ? "DESC" : "ASC";

        // Xóa hóa đơn trống khi xem danh sách hóa đơn
        if ("HoaDon".equals(m.table)) {
            new BookingRepository().deleteEmptyInvoices();
        }

        int page = Math.max(1, num(q.getParameter("page"), 1));
        int size = 8;
        int off = (page - 1) * size;
        boolean hasStatus = contains(m.cols, "trang_thai");

        // Xây dựng điều kiện WHERE
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if ("San".equals(m.table) || "HoaDon".equals(m.table)) {
            where.append(" AND trang_thai <> N'ĐÃ XÓA'");
        }
        // Lọc Voucher theo loai (1=HĐ, 2=Sân, 3=DV) nếu có tham số
        String loaiFilter = val(q, "loai");
        List<String> args = new ArrayList<>();
        if ("Voucher".equals(m.table) && !loaiFilter.isEmpty()) {
            where.append(" AND loai=?");
            args.add(loaiFilter);
        }
        buildSearchAndStatusQuery(where, args, m, search, status, hasStatus);

        String base = " FROM " + m.table + ("CTHD".equals(m.table) ? " JOIN HoaDon hd ON CTHD.ma_hd = hd.ma_hd " : "") + where;

        // Đếm tổng và lấy dữ liệu
        int total = countRecords(base, args);
        List<Map<String, Object>> rows = fetchRows(base, args, m, sort, dir, off, size);

        q.setAttribute("meta", m);
        q.setAttribute("rows", rows);
        q.setAttribute("totalPages", Math.max(1, (int) Math.ceil(total / (double) size)));
        q.setAttribute("page", page);
        q.setAttribute("hasStatus", hasStatus);

        // Khu vực "Voucher áp dụng" trên trang Sân / Dịch vụ
        attachEntityVouchers(q, m);

        q.getRequestDispatcher("/jsp/admin/list.jsp").forward(q, p);
    }

    /**
     * Gắn danh sách voucher theo loai vào request khi quản lý Sân (loai=2) hoặc Dịch vụ (loai=3).
     */
    private void attachEntityVouchers(HttpServletRequest q, Meta m) {
        try {
            if ("San".equals(m.table)) {
                q.setAttribute("entityVouchers", voucherService.getVoucherSanAsMaps());
                q.setAttribute("voucherLoai", VoucherLoai.SAN);
                q.setAttribute("voucherLoaiLabel", "Sân");
            } else if ("DichVu".equals(m.table)) {
                q.setAttribute("entityVouchers", voucherService.getVoucherDichVuAsMaps());
                q.setAttribute("voucherLoai", VoucherLoai.DICH_VU);
                q.setAttribute("voucherLoaiLabel", "Dịch vụ");
            }
        } catch (Exception e) {
            q.setAttribute("entityVouchers", List.of());
            System.err.println("attachEntityVouchers: " + e.getMessage());
        }
    }

    private void buildSearchAndStatusQuery(StringBuilder where, List<String> args, Meta m, String search, String status, boolean hasStatus) {
        if (!search.isEmpty()) {
            where.append(" AND (");
            for (int i = 0; i < m.cols.length; i++) {
                if (i > 0) where.append(" OR ");
                where.append("CAST(").append(m.cols[i]).append(" AS NVARCHAR(200)) LIKE ?");
                args.add("%" + search + "%");
            }
            where.append(")");
        }
        if (hasStatus && !status.isEmpty()) {
            where.append(" AND trang_thai=?");
            args.add(status);
        }
    }

    private int countRecords(String base, List<String> args) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT COUNT(*)" + base)) {
            bind(s, args);
            ResultSet r = s.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private List<Map<String, Object>> fetchRows(String base, List<String> args, Meta m, String sort, String dir, int off, int size) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String selectCols = "CTHD".equals(m.table) ? "CTHD.*, hd.trang_thai AS trang_thai_hd" : String.join(",", m.cols);
        String sql = "SELECT " + selectCols + base + " ORDER BY " + ("CTHD".equals(m.table) ? "CTHD." + sort : sort) + " " + dir + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, args);
            s.setInt(args.size() + 1, off);
            s.setInt(args.size() + 2, size);
            ResultSet r = s.executeQuery();
            while (r.next()) {
                Map<String, Object> x = new LinkedHashMap<>();
                for (String col : m.cols) x.put(col, r.getObject(col));
                if ("CTHD".equals(m.table)) x.put("trang_thai_hd", r.getObject("trang_thai_hd"));
                rows.add(x);
            }
        }
        return rows;
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            Meta m = meta(q);
            String op = q.getParameter("op");
            String id = q.getParameter(m.pk);
            
            // Xử lý hành động Xóa
            if ("delete".equals(op)) {
                if ("Voucher".equals(m.table)) {
                    voucherService.delete(id);
                    // Quay lại trang Sân/Dịch vụ nếu xóa từ khu vực voucher
                    String back = q.getParameter("back");
                    if (back != null && !back.isBlank()) {
                        p.sendRedirect(q.getContextPath() + "/admin/manage?entity=" + back + "&ok=1");
                        return;
                    }
                } else if ("San".equals(m.table)) {
                    // Xóa mềm cho Sân: Cập nhật trạng thái thành 'ĐÃ XÓA'
                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement s = c.prepareStatement("UPDATE San SET trang_thai=N'ĐÃ XÓA' WHERE ma_san=?")) {
                        s.setString(1, id);
                        s.executeUpdate();
                    }
                } else if ("HoaDon".equals(m.table)) {
                    // Xóa mềm cho Hóa đơn: Giải phóng sân và đánh dấu 'ĐÃ XÓA'
                    try (Connection c = DBConnection.getConnection()) {
                        c.setAutoCommit(false);
                        try {
                            // 1. Giải phóng các sân trong LichSan liên quan đến hóa đơn này
                            try (PreparedStatement s = c.prepareStatement(
                                    "UPDATE LichSan SET trang_thai=N'CÒN TRỐNG' WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd=?)")) {
                                s.setString(1, id);
                                s.executeUpdate();
                            }
                            // 2. Đánh dấu hóa đơn là ĐÃ XÓA
                            try (PreparedStatement s = c.prepareStatement("UPDATE HoaDon SET trang_thai=N'ĐÃ XÓA' WHERE ma_hd=?")) {
                                s.setString(1, id);
                                s.executeUpdate();
                            }
                            c.commit();
                        } catch (Exception ex) {
                            c.rollback();
                            throw ex;
                        } finally {
                            c.setAutoCommit(true);
                        }
                    }
                } else {
                    // Xóa vật lý cho các thực thể khác
                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement s = c.prepareStatement("DELETE FROM " + m.table + " WHERE " + m.pk + "=?")) {
                        s.setString(1, id);
                        s.executeUpdate();
                    }
                }
            } else {
                // Xử lý Thêm mới hoặc Cập nhật
                if ("Voucher".equals(m.table)) {
                    saveVoucher(q, "create".equals(op));
                    String back = q.getParameter("back");
                    if (back != null && !back.isBlank()) {
                        p.sendRedirect(q.getContextPath() + "/admin/manage?entity=" + back + "&ok=1");
                        return;
                    }
                } else {
                    save(q, m, "create".equals(op));
                }
            }
            // Nếu người dùng bấm "Lưu & Thanh toán", chuyển sang form thanh toán sau khi lưu
            String afterSave = q.getParameter("afterSave");
            if ("payment".equals(afterSave) && "HoaDon".equals(m.table)) {
                String savedId = q.getParameter(m.pk);
                if (savedId != null && !savedId.isBlank()) {
                    p.sendRedirect(q.getContextPath() + "/admin/invoice-management?op=paymentForm&id=" + savedId + "&saved=1");
                    return;
                }
            }
            String redirectLoai = q.getParameter("loai");
            String extra = ("Voucher".equals(m.table) && redirectLoai != null && !redirectLoai.isBlank())
                    ? "&loai=" + redirectLoai : "";
            p.sendRedirect(q.getContextPath() + "/admin/manage?entity=" + m.table + "&ok=1" + extra);
        } catch (Exception e) {
            q.setAttribute("error", friendly(e));
            doGet(q, p);
        }
    }

    /**
     * Lưu Voucher qua VoucherService (đảm bảo map đầy đủ cột loai).
     */
    private void saveVoucher(HttpServletRequest q, boolean create) throws Exception {
        Voucher v = new Voucher();
        v.setMaVoucher(trim(q.getParameter("ma_voucher")));
        v.setMaCode(trim(q.getParameter("ma_code")));
        v.setTenVoucher(trim(q.getParameter("ten_voucher")));
        v.setLoaiGiam(trim(q.getParameter("loai_giam")));
        if (v.getLoaiGiam() == null || v.getLoaiGiam().isBlank()) v.setLoaiGiam("MONEY");
        v.setGiaTri(parseDoubleSafe(q.getParameter("gia_tri")));
        v.setDonToiThieu(parseDoubleSafe(q.getParameter("don_toi_thieu")));
        v.setNgayBatDau(parseDate(q.getParameter("ngay_bat_dau")));
        v.setNgayKetThuc(parseDate(q.getParameter("ngay_ket_thuc")));
        v.setSoLuong(parseIntSafe(q.getParameter("so_luong")));
        String st = trim(q.getParameter("trang_thai"));
        v.setTrangThai(st == null || st.isBlank() ? "HOẠT ĐỘNG" : st);
        int loai = parseIntSafe(q.getParameter("loai"));
        if (!VoucherLoai.isValid(loai)) loai = VoucherLoai.HOA_DON;
        v.setLoai(loai);

        if (create) {
            if (v.getMaVoucher() == null || v.getMaVoucher().isBlank()) {
                throw new IllegalArgumentException("Mã voucher không được trống");
            }
            voucherService.insert(v);
        } else {
            voucherService.update(v);
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (Exception e) { return null; }
    }

    /**
     * Lưu hoặc cập nhật dữ liệu một bản ghi.
     */
    private void save(HttpServletRequest q, Meta m, boolean create) throws SQLException {
        List<String> cols = new ArrayList<>(Arrays.asList(m.cols));
        if (!create || "CTHD".equals(m.table)) cols.remove(m.pk);

        Map<String, String> vals = new LinkedHashMap<>();
        for (String col : cols) {
            String v = q.getParameter(col);
            vals.put(col, v == null ? null : v.trim());
        }

        // Xử lý logic nghiệp vụ đặc thù
        if ("HoaDon".equals(m.table)) {
            processHoaDonSave(q, m, vals, create);
        } else if ("CTHD".equals(m.table)) {
            processCthdSave(vals);
        } else if ("LichSan".equals(m.table)) {
            vals.put("trang_thai", sanitizeStatus(vals.get("trang_thai"), LICH_SAN_STATUS, "CÒN TRỐNG"));
        } else if ("San".equals(m.table)) {
            vals.put("trang_thai", sanitizeStatus(vals.get("trang_thai"), SAN_STATUS, "HOẠT ĐỘNG"));
        }

        executeSaveQuery(m, cols, vals, q.getParameter(m.pk), create);
    }

    private void processHoaDonSave(HttpServletRequest q, Meta m, Map<String, String> vals, boolean create) throws SQLException {
        String st = sanitizeStatus(vals.get("trang_thai"), INVOICE_STATUS, "CHƯA THANH TOÁN");
        vals.put("trang_thai", st);

        if (!create) {
            String id = q.getParameter(m.pk);
            if (id != null) {
                updateHoaDonFull(q, id, st, vals);
            }
        } else {
            double tong = parseDoubleSafe(vals.get("tong_tien"));
            double coc = tong * 0.3;
            double remain = "ĐÃ THANH TOÁN".equals(st) ? 0d : Math.max(0d, tong - coc);
            vals.put("tien_coc", toDecimalString(coc));
            vals.put("so_tien_con_lai", toDecimalString(remain));
        }
    }

    private void updateHoaDonFull(HttpServletRequest q, String id, String st, Map<String, String> vals) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Cập nhật sân trước để đảm bảo có sân gắn dịch vụ
                updateHoaDonSlots(q, id, conn);
                // Sau đó mới cập nhật dịch vụ
                updateHoaDonServices(q, id, conn);
                recalculateHoaDonTotals(id, st, vals, conn);
                syncLichSanStatus(id, st, conn);
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw new SQLException(ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void updateHoaDonServices(HttpServletRequest q, String id, Connection conn) throws SQLException {
        Map<String, Integer> services = new HashMap<>();
        Enumeration<String> params = q.getParameterNames();
        while (params.hasMoreElements()) {
            String p = params.nextElement();
            if (p.startsWith("service_") && "on".equals(q.getParameter(p))) {
                String svId = p.substring(8);
                int qty = parseIntSafe(q.getParameter("qty_" + svId));
                services.put(svId, Math.max(1, qty));
            }
        }

        String maLich = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT TOP 1 ma_lich FROM CTHD WHERE ma_hd=? AND ma_dv IS NULL")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) maLich = rs.getString(1);
        }

        if (maLich != null) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CTHD WHERE ma_hd=? AND ma_dv IS NOT NULL")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            for (Map.Entry<String, Integer> e : services.entrySet()) {
                double pr = getServicePrice(e.getKey(), conn);
                insertCthdService(id, maLich, e.getKey(), e.getValue(), pr, conn);
            }
        }
    }

    private void updateHoaDonSlots(HttpServletRequest q, String id, Connection conn) throws SQLException {
        // Thu thập danh sách các sân/ca được chọn trong form (dạng add_slot_<ma_san>_<so_ca>)
        Set<String> selectedKeys = new HashSet<>();
        Enumeration<String> params = q.getParameterNames();
        while (params.hasMoreElements()) {
            String p = params.nextElement();
            if (p.startsWith("add_slot_") && "on".equals(q.getParameter(p))) {
                selectedKeys.add(p.substring(9));
            }
        }

        // Lấy ngày đặt của hóa đơn
        java.sql.Date invoiceDate = getInvoiceDate(conn, id);

        // Lấy danh sách sân/ca hiện có trong hóa đơn (dạng ma_san_so_ca)
        Set<String> existingKeys = new HashSet<>();
        Map<String, String> keyToLichId = new HashMap<>();
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT l.ma_lich, l.ma_san, l.so_ca FROM CTHD c " +
                "JOIN LichSan l ON l.ma_lich=c.ma_lich " +
                "WHERE c.ma_hd=? AND c.ma_dv IS NULL")) {
            s.setString(1, id);
            ResultSet r = s.executeQuery();
            while (r.next()) {
                String key = r.getString("ma_san") + "_" + r.getInt("so_ca");
                existingKeys.add(key);
                keyToLichId.put(key, r.getString("ma_lich"));
            }
        }

        // Xóa các sân không còn được chọn trong hóa đơn
        for (String oldKey : existingKeys) {
            if (!selectedKeys.contains(oldKey)) {
                String oldLichId = keyToLichId.get(oldKey);
                // Xóa khỏi CTHD
                try (PreparedStatement s = conn.prepareStatement(
                        "DELETE FROM CTHD WHERE ma_hd=? AND ma_lich=? AND ma_dv IS NULL")) {
                    s.setString(1, id);
                    s.setString(2, oldLichId);
                    s.executeUpdate();
                }
                // Giải phóng sân nếu không còn thuộc hóa đơn nào khác
                try (PreparedStatement s = conn.prepareStatement(
                        "UPDATE LichSan SET trang_thai = N'CÒN TRỐNG' WHERE ma_lich=? AND NOT EXISTS (SELECT 1 FROM CTHD WHERE ma_lich = ? AND ma_dv IS NULL AND ma_hd <> ?)")) {
                    s.setString(1, oldLichId);
                    s.setString(2, oldLichId);
                    s.setString(3, id);
                    s.executeUpdate();
                }
            }
        }

        // Thêm các sân mới được chọn
        for (String key : selectedKeys) {
            if (existingKeys.contains(key)) continue; // Đã có trong hóa đơn
            String[] parts = key.split("_", 2);
            if (parts.length != 2) continue;
            String maSan = parts[0];
            int soCa = parseIntSafe(parts[1]);

            // Tìm hoặc tạo bản ghi LichSan cho sân/ca này
            String lichId = findOrCreateLichSan(conn, maSan, soCa, invoiceDate);

            double price = getFieldPrice(conn, maSan);
            try (PreparedStatement s = conn.prepareStatement("INSERT INTO CTHD(ma_hd, ma_lich, ma_dv, so_luong, don_gia, thanh_tien) VALUES(?,?,NULL,1,?,?)")) {
                s.setString(1, id);
                s.setString(2, lichId);
                s.setDouble(3, price);
                s.setDouble(4, price);
                s.executeUpdate();
            }
            try (PreparedStatement s = conn.prepareStatement("UPDATE LichSan SET trang_thai = N'ĐANG GIỮ CHỖ' WHERE ma_lich = ?")) {
                s.setString(1, lichId);
                s.executeUpdate();
            }
        }
    }

    /**
     * Lấy ngày đặt của hóa đơn (từ slot đầu tiên hoặc ngày hôm nay).
     */
    private java.sql.Date getInvoiceDate(Connection conn, String id) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT TOP 1 l.ngay_dat FROM CTHD c JOIN LichSan l ON l.ma_lich=c.ma_lich WHERE c.ma_hd=? AND c.ma_dv IS NULL")) {
            s.setString(1, id);
            ResultSet r = s.executeQuery();
            if (r.next()) {
                java.sql.Date d = r.getDate(1);
                if (d != null) return d;
            }
        }
        return java.sql.Date.valueOf(LocalDate.now());
    }

    /**
     * Tìm bản ghi LichSan theo sân/ca/ngày, nếu chưa có thì tạo mới.
     */
    private String findOrCreateLichSan(Connection conn, String maSan, int soCa, java.sql.Date ngayDat) throws SQLException {
        // Tìm bản ghi LichSan đã tồn tại
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT ma_lich FROM LichSan WHERE ma_san=? AND ngay_dat=? AND so_ca=?")) {
            s.setString(1, maSan);
            s.setDate(2, ngayDat);
            s.setInt(3, soCa);
            ResultSet r = s.executeQuery();
            if (r.next()) return r.getString(1);
        }

        // Tạo mới bản ghi LichSan
        String lichId = nextLichId(conn);
        String[] sl = Slots.get(soCa);
        LocalTime start = LocalTime.parse(sl[2]);
        LocalTime end = LocalTime.parse(sl[3]);
        LocalDateTime startDt = ngayDat.toLocalDate().atTime(start);
        LocalDateTime endDt = ngayDat.toLocalDate().atTime(end);
        if (!end.isAfter(start)) endDt = endDt.plusDays(1);

        try (PreparedStatement s = conn.prepareStatement(
                "INSERT INTO LichSan(ma_lich,ma_san,ca,gio_bat_dau,gio_ket_thuc,trang_thai,ngay_dat,buoi,so_ca) VALUES(?,?,?,?,?,?,?,?,?)")) {
            s.setString(1, lichId);
            s.setString(2, maSan);
            s.setString(3, sl[2] + " - " + sl[3]);
            s.setTimestamp(4, Timestamp.valueOf(startDt));
            s.setTimestamp(5, Timestamp.valueOf(endDt));
            s.setString(6, "CÒN TRỐNG");
            s.setDate(7, ngayDat);
            s.setString(8, sl[0]);
            s.setInt(9, soCa);
            s.executeUpdate();
        }
        return lichId;
    }

    /**
     * Sinh mã lịch sân tự tăng tiếp theo.
     */
    private String nextLichId(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet r = s.executeQuery("SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(ma_lich,2,20))),0)+1 FROM LichSan")) {
            r.next();
            return "L" + String.format("%03d", r.getInt(1));
        }
    }

    /**
     * Lấy giá thuê của một sân (giá theo giờ * 2 giờ/ca).
     */
    private double getFieldPrice(Connection conn, String maSan) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement("SELECT gia_theo_gio FROM San WHERE ma_san=?")) {
            s.setString(1, maSan);
            ResultSet r = s.executeQuery();
            if (r.next()) return r.getDouble(1) * 2;
        }
        return 600000;
    }

    private double getServicePrice(String svId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT don_gia FROM DichVu WHERE ma_dv=?")) {
            ps.setString(1, svId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private void insertCthdService(String hdId, String lichId, String svId, int qty, double price, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, hdId);
            ps.setString(2, lichId);
            ps.setString(3, svId);
            ps.setInt(4, qty);
            ps.setDouble(5, price);
            ps.setDouble(6, price * qty);
            ps.executeUpdate();
        }
    }

    private void recalculateHoaDonTotals(String id, String st, Map<String, String> vals, Connection conn) throws SQLException {
        double basePrice = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(thanh_tien) FROM CTHD WHERE ma_hd=? AND ma_dv IS NULL")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) basePrice = rs.getDouble(1);
        }
        double svTotal = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(thanh_tien) FROM CTHD WHERE ma_hd=? AND ma_dv IS NOT NULL")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) svTotal = rs.getDouble(1);
        }
        double tong = basePrice + svTotal;
        double coc = tong * 0.3;
        double remain = "ĐÃ THANH TOÁN".equals(st) ? 0d : Math.max(0d, tong - coc);

        vals.put("tien_coc", toDecimalString(coc));
        vals.put("tong_tien", toDecimalString(tong));
        vals.put("so_tien_con_lai", toDecimalString(remain));
    }

    private void syncLichSanStatus(String id, String st, Connection conn) throws SQLException {
        String lsSt = ("ĐÃ HỦY".equals(st) || "ĐÃ XÓA".equals(st)) ? "CÒN TRỐNG" : ("ĐÃ THANH TOÁN".equals(st) || "MỚI THANH TOÁN CỌC".equals(st) || "ĐÃ ĐẶT".equals(st) ? "ĐÃ ĐẶT" : null);
        if (lsSt != null) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE LichSan SET trang_thai=? WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd=?)")) {
                ps.setString(1, lsSt);
                ps.setString(2, id);
                ps.executeUpdate();
            }
        }
    }

    private void processCthdSave(Map<String, String> vals) {
        int sl = parseIntSafe(vals.get("so_luong"));
        double donGia = parseDoubleSafe(vals.get("don_gia"));
        vals.put("thanh_tien", toDecimalString(sl * donGia));
    }

    private void executeSaveQuery(Meta m, List<String> cols, Map<String, String> vals, String pkValue, boolean create) throws SQLException {
        StringBuilder sql = new StringBuilder(create ? "INSERT INTO " + m.table + " (" : "UPDATE " + m.table + " SET ");
        if (create) sql.append(String.join(",", cols)).append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append(create ? "?" : cols.get(i) + "=?");
        }
        sql.append(create ? ")" : " WHERE " + m.pk + "=?");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql.toString())) {
            int i = 1;
            for (String col : cols) {
                String v = vals.get(col);
                if (v == null || v.isEmpty()) s.setNull(i++, Types.VARCHAR);
                else s.setString(i++, v);
            }
            if (!create) s.setString(i, pkValue);
            s.executeUpdate();
        }
    }

    /**
     * Tìm kiếm một bản ghi cụ thể theo khóa chính.
     */
    private Map<String, Object> find(Meta m, String id) throws SQLException {
        if (id == null) return null;
        if ("Voucher".equals(m.table)) {
            try {
                Voucher v = voucherService.getById(id);
                return v != null ? voucherService.toMap(v) : null;
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT " + String.join(",", m.cols) + " FROM " + m.table + " WHERE " + m.pk + "=?")) {
            s.setString(1, id);
            ResultSet r = s.executeQuery();
            if (!r.next()) return null;
            Map<String, Object> x = new LinkedHashMap<>();
            for (String col : m.cols) x.put(col, r.getObject(col));
            return x;
        }
    }

    /**
     * Gán các tham số vào PreparedStatement.
     */
    private static void bind(PreparedStatement s, List<String> a) throws SQLException {
        for (int i = 0; i < a.size(); i++) s.setString(i + 1, a.get(i));
    }

    /**
     * Lấy giá trị từ tham số request và loại bỏ khoảng trắng thừa.
     */
    private static String val(HttpServletRequest q, String k) {
        String v = q.getParameter(k);
        return v == null ? "" : v.trim();
    }

    /**
     * Chuyển đổi chuỗi sang số nguyên an toàn.
     */
    private static int num(String v, int d) {
        try { return Integer.parseInt(v); }
        catch (Exception e) { return d; }
    }

    private static int parseIntSafe(String v) {
        return num(v, 0);
    }

    /**
     * Kiểm tra một giá trị có tồn tại trong mảng chuỗi hay không.
     */
    private static boolean contains(String[] a, String x) {
        for (String v : a) if (v.equals(x)) return true;
        return false;
    }

    /**
     * Kiểm tra và chuẩn hóa giá trị trạng thái.
     */
    private static String sanitizeStatus(String v, Set<String> allowed, String def) {
        if (v == null) return def;
        String t = v.trim();
        return allowed.contains(t) ? t : def;
    }

    /**
     * Chuyển đổi chuỗi sang số thực an toàn.
     */
    private static double parseDoubleSafe(String v) {
        try { return v == null || v.trim().isEmpty() ? 0d : Double.parseDouble(v.trim()); }
        catch (Exception e) { return 0d; }
    }

    /**
     * Chuyển đổi số thực sang định dạng chuỗi số thập phân để lưu vào CSDL.
     */
    private static String toDecimalString(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return "0";
        return String.valueOf(x);
    }

    private static String friendly(Exception e) {
        return "Không thể lưu dữ liệu. Kiểm tra mã trùng hoặc khóa liên kết.";
    }
}
```

**Giải thích:**
- AdminCrudServlet là Servlet tổng quát xử lý CRUD cho nhiều bảng
- Người 1 chỉ cần quan tâm phần `KhachHang` và `NhanVien`
- Các bảng khác (San, DichVu, HoaDon...) do người khác phụ trách

---

## 6. CÁC JSP

### 6.1 login.jsp (Trang đăng nhập Admin)
**Đường dẫn:** `/jsp/admin/login.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container" style="max-width:400px;">
<h2>Đăng nhập Quản trị</h2>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<div class="card">
<form method="post">
<label>Số điện thoại</label>
<input type="tel" name="sdt" value="${empty sdt ? '0365365113' : sdt}">
<label>Mật khẩu</label>
<input type="password" name="password" value="1">
<p><button class="btn">Đăng nhập</button>
<button type="button" class="btn" id="manualLogin">Tự động</button></p>
</form>
<p><small>Tài khoản: 0365365113 / 1</small></p>
<p><a href="${pageContext.request.contextPath}/home">&larr; Về trang chủ</a></p>
</div>
</div>
<script>document.getElementById('manualLogin')?.addEventListener('click',function(){document.querySelector('form').submit();});</script>
<%@ include file="/jsp/common/footer.jspf" %>
```

**Giải thích:**
- Form gửi POST đến `/admin/login`
- `AdminLoginServlet` sẽ xử lý và tự động đăng nhập với tài khoản demo nếu không có tham số
- Sau khi đăng nhập thành công, chuyển đến `/admin` (Dashboard)

### 6.2 dashboard.jsp (Trang chủ Admin)
**Đường dích:** `/jsp/admin/dashboard.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.dashboard-stats{display:flex;flex-wrap:wrap;gap:15px;margin:20px 0}
.stat-card{flex:1;min-width:150px;background:#fff;border:1px solid #e0e0e0;border-radius:8px;padding:20px;text-align:center;box-shadow:0 2px 4px rgba(0,0,0,0.05);transition:all 0.2s}
.stat-card:hover{transform:translateY(-5px);box-shadow:0 4px 12px rgba(42,82,152,0.2)}
.stat-value{font-size:28px;font-weight:bold;color:#1a3d7a;margin:10px 0}
.stat-label{color:#666;font-size:14px}
.management-grid{display:flex;flex-wrap:wrap;gap:10px;margin:20px 0}
.management-card{flex:1;min-width:120px;background:#2a5298;color:#fff;padding:15px;border-radius:8px;text-align:center;text-decoration:none;transition:all 0.2s}
.management-card:hover{background:#1a3d7a;transform:translateY(-3px)}
</style>
<div class="container">
<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px">
<div><h2 style="margin:0;color:#1a3d7a">Bảng điều khiển</h2>
<p style="color:#666">Xin chào, ${sessionScope.adminUser}</p></div>
<a class="btn btn-danger" href="${pageContext.request.contextPath}/admin/logout">Đăng xuất</a>
</div>

<!-- Statistics Cards -->
<div class="dashboard-stats">
<c:forEach items="${stats}" var="x">
<div class="stat-card">
<div class="stat-value">${x.value}</div>
<div class="stat-label">${x.key}</div>
</div>
</c:forEach>
</div>

<h3 style="color:#1a3d7a;margin-top:25px">Quản lý</h3>
<div class="management-grid">
<c:forEach items="${['San','LichSan','KhachHang','NhanVien','DichVu','HoaDon','CTHD']}" var="e">
<a class="management-card" href="${pageContext.request.contextPath}/admin/manage?entity=${e}">${L[e]}</a>
</c:forEach>
<a class="management-card" style="background:#4caf50" href="${pageContext.request.contextPath}/admin/field-dashboard">Quản lý Sân</a>
</div>

</div>
<%@ include file="/jsp/common/footer.jspf" %>
```

### 6.3 form.jsp (Form thêm/sửa Khách hàng/Nhân viên)
**Đường dẫn:** `/jsp/admin/form.jsp`

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container" style="max-width:500px;">
<c:set var="isFinalStatus" value="${(meta.table=='HoaDon' && (record['trang_thai']=='ĐÃ THANH TOÁN' || record['trang_thai']=='ĐÃ HỦY')) || (meta.table=='CTHD' && (invoiceStatus=='ĐÃ THANH TOÁN' || invoiceStatus=='ĐÃ HỦY'))}"/>
<c:set var="isInvoice" value="${meta.table=='HoaDon'}"/>
<p><a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}">&larr; Danh sách</a></p>
<h2>${isNew?'Thêm':'Sửa'} ${L[meta.table]}</h2>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<c:if test="${not empty param.saved && param.saved=='1'}">
    <div class="alert" style="background:#e8f5e9;color:#2e7d32;border:1px solid #4caf50;">✓ Đã cập nhật hóa đơn thành công!</div>
</c:if>
<div class="card">
<form method="post">
<input type="hidden" name="entity" value="${meta.table}">
<input type="hidden" name="op" value="${isNew?'create':'update'}">
<c:if test="${not empty param.back}"><input type="hidden" name="back" value="${param.back}"></c:if>
<c:if test="${not empty param.loai && meta.table=='Voucher'}"><input type="hidden" name="loai_pref" value="${param.loai}"></c:if>
<c:forEach items="${meta.cols}" var="col">
<label>${L[col]!=null?L[col]:col}</label>
<c:choose>
    <c:when test="${!isNew&&col==meta.pk}"><input name="${col}" value="${record[col]}" readonly style="background:#eee"></c:when>
    <c:when test="${col=='trang_thai'}">
    <c:choose>
      <c:when test="${isInvoice}">
        <c:choose>
          <c:when test="${record[col]=='ĐÃ THANH TOÁN' || record[col]=='ĐÃ HỦY'}">
            <input name="${col}" value="${record[col]}" readonly style="background:#eee">
          </c:when>
          <c:otherwise>
            <select name="${col}">
              <option value="ĐÃ ĐẶT" ${record[col]=='ĐÃ ĐẶT'?'selected':''}>Đã đặt</option>
              <option value="CHƯA THANH TOÁN" ${record[col]=='CHƯA THANH TOÁN'?'selected':''}>Chưa thanh toán</option>
              <option value="MỚI THANH TOÁN CỌC" ${record[col]=='MỚI THANH TOÁN CỌC'?'selected':''}>Mới thanh toán cọc</option>
              <option value="ĐÃ THANH TOÁN" ${record[col]=='ĐÃ THANH TOÁN'?'selected':''}>Đã thanh toán</option>
              <option value="ĐÃ HỦY" ${record[col]=='ĐÃ HỦY'?'selected':''}>Đã hủy</option>
            </select>
          </c:otherwise>
        </c:choose>
      </c:when>
      <c:when test="${meta.table=='LichSan'}">
        <select name="${col}">
          <option value="CÒN TRỐNG" ${record[col]=='CÒN TRỐNG'?'selected':''}>Còn trống</option>
          <option value="ĐANG GIỮ CHỖ" ${record[col]=='ĐANG GIỮ CHỖ'?'selected':''}>Đang giữ chỗ</option>
          <option value="ĐÃ ĐẶT" ${record[col]=='ĐÃ ĐẶT'?'selected':''}>Đã đặt</option>
          <option value="SỰ KIỆN" ${record[col]=='SỰ KIỆN'?'selected':''}>Sự kiện</option>
        </select>
      </c:when>
      <c:when test="${meta.table=='San'}">
        <select name="${col}">
          <option value="HOẠT ĐỘNG" ${record[col]=='HOẠT ĐỘNG'?'selected':''}>Hoạt động</option>
          <option value="BẢO TRÌ" ${record[col]=='BẢO TRÌ'?'selected':''}>Bảo trì</option>
        </select>
      </c:when>
      <c:otherwise>
        <input name="${col}" value="${record[col]}" ${isNew?'required':''}>
      </c:otherwise>
    </c:choose>
    </c:when>
    <c:when test="${col=='tong_tien' || col=='so_tien_con_lai'}">
        <c:choose>
            <c:when test="${meta.table=='HoaDon'}">
                <c:choose>
                    <c:when test="${isFinalStatus}">
                        <div style="margin-bottom: 5px;">
                            <label style="display:inline-block;width:120px;">${L[col]}:</label>
                            <b><fmt:formatNumber value="${record[col]}" type="number"/>đ</b>
                        </div>
                        <c:if test="${col=='so_tien_con_lai'}">
                            <div style="margin-top:20px;">
                                <h4>Danh sách sân/ca</h4>
                                <table class="table" style="width:100%; border-collapse: collapse;" border="1">
                                    <thead style="background:#eee;">
                                        <tr><th>Tên sân</th><th>Ca</th><th>Đơn giá</th><th>Thành tiền</th></tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${inv.slots}" var="sl">
                                            <tr>
                                                <td>${sl.ten_san}</td>
                                                <td>${sl.buoi} ${sl.ca} (${sl.ngay_dat})</td>
                                                <td><fmt:formatNumber value="${sl.thanh_tien}" type="number"/>đ</td>
                                                <td><fmt:formatNumber value="${sl.thanh_tien}" type="number"/>đ</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>

                                <h4 style="margin-top:20px;">Danh sách dịch vụ</h4>
                                <table class="table" style="width:100%; border-collapse: collapse;" border="1">
                                    <thead style="background:#eee;">
                                        <tr><th>Tên dịch vụ</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${inv.services}" var="sv">
                                            <tr>
                                                <td>${sv.ten_dv}</td>
                                                <td>${sv.so_luong}</td>
                                                <td><fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</td>
                                                <td><fmt:formatNumber value="${sv.thanh_tien}" type="number"/>đ</td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty inv.services}">
                                            <tr><td colspan="4" style="text-align:center;">Không có dịch vụ</td></tr>
                                        </c:if>
                                    </tbody>
                                </table>
                                <div style="margin-top:15px; text-align:right; border-top:2px solid #333; padding-top:10px;">
                                    <p>Tổng tiền: <b><fmt:formatNumber value="${record['tong_tien']}" type="number"/>đ</b></p>
                                    <p>Đã thanh toán: <b><fmt:formatNumber value="${record['tong_tien'] - record['so_tien_con_lai']}" type="number"/>đ</b></p>
                                    <p>Còn lại: <b style="color:red;"><fmt:formatNumber value="${record['so_tien_con_lai']}" type="number"/>đ</b></p>
                                </div>
                            </div>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <input name="${col}" value="${record[col]}" readonly style="background:#eee">
                        <c:if test="${col=='so_tien_con_lai'}">
                            <div style="margin: 15px 0; padding: 10px; border: 1px solid #ccc; background: #f9f9f9;">
                                <b>Dịch vụ đi kèm:</b>
                                <div style="max-height: 180px; overflow-y: auto; margin-top: 5px;">
                                <c:forEach items="${services}" var="sv">
                                    <c:set var="isSelected" value="false"/>
                                    <c:if test="${not empty editServices}">
                                        <c:forEach items="${editServices}" var="esId">
                                            <c:if test="${esId == sv.ma_dv}"><c:set var="isSelected" value="true"/></c:if>
                                        </c:forEach>
                                    </c:if>
                                    <c:set var="savedQty" value="${not empty editServiceQty ? editServiceQty[sv.ma_dv] : 1}"/>
                                    <div style="margin: 5px 0; display: flex; align-items: center; gap: 10px;">
                                        <input type="checkbox" name="service_${sv.ma_dv}" ${isSelected ? 'checked' : ''} style="width:18px;height:18px;">
                                        <label style="margin:0;font-weight:normal;">${sv.ten_dv} — <fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</label>
                                        <input type="number" name="qty_${sv.ma_dv}" value="${savedQty}" min="1" max="10" style="width:50px;padding:4px;">
                                    </div>
                                </c:forEach>
                                <c:if test="${empty services}">
                                    <p style="font-size: 0.9em; color: #666;">Không có dịch vụ</p>
                                </c:if>
                                </div>
                            </div>
                            
                            <div style="margin: 15px 0; padding: 10px; border: 1px solid #cce5ff; background: #e7f3ff;">
                                <b>Chọn sân/ca trong ngày ${not empty inv.slots ? inv.slots[0].ngay_dat : ''}:</b>
                                <div style="max-height: 220px; overflow-y: auto; margin-top: 5px;">
                                    <c:forEach items="${availableSlots}" var="slot">
                                        <div style="margin: 5px 0; display: flex; align-items: center; gap: 10px; padding: 4px; border-bottom: 1px dashed #d0e8ff;">
                                            <input type="checkbox" name="add_slot_${slot.ma_san}_${slot.so_ca}" ${slot.checked ? 'checked' : ''} style="width:18px;height:18px;">
                                            <label style="margin:0;font-weight:normal;cursor:pointer;">
                                                <b>${slot.ten_san}</b> - Ca ${slot.so_ca} 
                                                (${slot.gio_bat_dau} - ${slot.gio_ket_thuc})
                                                <c:if test="${slot.checked}"><span style="color:#2e7d32;font-weight:bold;margin-left:5px;">✓</span></c:if>
                                            </label>
                                        </div>
                                    </c:forEach>
                                    <c:if test="${empty availableSlots}">
                                        <p style="font-size: 0.9em; color: #666;">Không có sân/ca trống nào trong ngày này.</p>
                                    </c:if>
                                </div>
                            </div>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <input name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="${col=='email'}"><input type="email" name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}></c:when>
    <c:when test="${col=='ngay_dat' || col=='ngayBatDau' || col=='ngayKetThuc' || col=='ngay_bat_dau' || col=='ngay_ket_thuc'}"><input type="date" name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}></c:when>
    <c:when test="${meta.table=='Voucher' && col=='loai_giam'}">
        <select name="${col}">
            <option value="MONEY" ${record[col]=='MONEY' || record[col]=='GIAM_TRUC_TIEP'?'selected':''}>Giảm tiền (MONEY)</option>
            <option value="PERCENT" ${record[col]=='PERCENT' || record[col]=='GIAM_THEO_PHAN_TRAM'?'selected':''}>Giảm % (PERCENT)</option>
        </select>
    </c:when>
    <c:when test="${meta.table=='Voucher' && col=='loai'}">
        <select name="${col}">
            <option value="1" ${record[col]==1 || record[col]=='1'?'selected':''}>1 — Hóa đơn (Booking)</option>
            <option value="2" ${record[col]==2 || record[col]=='2'?'selected':''}>2 — Sân</option>
            <option value="3" ${record[col]==3 || record[col]=='3'?'selected':''}>3 — Dịch vụ</option>
        </select>
    </c:when>
    <c:when test="${meta.table=='Voucher' && col=='trang_thai'}">
        <select name="${col}">
            <option value="HOẠT ĐỘNG" ${record[col]=='HOẠT ĐỘNG'?'selected':''}>HOẠT ĐỘNG</option>
            <option value="NGỪNG" ${record[col]=='NGỪNG'?'selected':''}>NGỪNG</option>
        </select>
    </c:when>
    <c:when test="${meta.table=='Voucher' && (col=='gia_tri' || col=='don_toi_thieu' || col=='so_luong')}">
        <input type="number" name="${col}" value="${record[col]}" step="any" ${isNew?'required':''}>
    </c:when>
    <c:otherwise><input name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}></c:otherwise>
    </c:choose>
</c:forEach>
<p><c:if test="${!isFinalStatus}"><button class="btn">Lưu</button></c:if>
<c:if test="${isInvoice && !isNew && (record.trang_thai=='ĐÃ ĐẶT' || record.trang_thai=='CHƯA THANH TOÁN' || record.trang_thai=='MỚI THANH TOÁN CỌC')}">
    <button type="submit" name="afterSave" value="payment" class="btn" style="background:#2e7d32;color:white;" onclick="return confirm('Hóa đơn sẽ được lưu trước khi chuyển sang thanh toán. Tiếp tục?')">Lưu & Thanh toán</button>
</c:if>
<a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}">Hủy</a></p>
</form></div></div>
<%@ include file="/jsp/common/footer.jspf" %>
```

### 6.4 list.jsp (Danh sách Khách hàng/Nhân viên)
**Đường dẫn:** `/jsp/admin/list.jsp`

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container">
<p><a class="btn" href="${pageContext.request.contextPath}/admin">&larr; Dashboard</a>
<a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}&action=form">+ Thêm</a></p>
<h2>Quản lý ${L[meta.table]}</h2>
<p>
<c:forEach items="${['San','LichSan','KhachHang','NhanVien','DichVu','HoaDon','CTHD','Voucher']}" var="e">
<a class="btn" href="?entity=${e}">${L[e]}</a>
</c:forEach></p>
<form style="margin:10px 0;"><input type="hidden" name="entity" value="${meta.table}">
<input type="text" name="search" value="${param.search}" placeholder="Tìm..." style="width:200px;display:inline;">
<button class="btn">Tìm</button></form>
<div style="overflow-x:auto;">
<table><thead><tr>
<c:forEach items="${meta.cols}" var="col"><th>${L[col]!=null?L[col]:col}</th></c:forEach>
<th>Thao tác</th></tr></thead>
<tbody>
<c:forEach items="${rows}" var="row">
<tr><c:forEach items="${meta.cols}" var="col"><td>${row[col]}</td></c:forEach>
<td>
<c:choose>
  <c:when test="${(meta.table=='HoaDon' && (row.trang_thai=='ĐÃ THANH TOÁN' || row.trang_thai=='ĐÃ HỦY')) || (meta.table=='CTHD' && (row.trang_thai_hd=='ĐÃ THANH TOÁN' || row.trang_thai_hd=='ĐÃ HỦY'))}">
    <a class="btn" href="?entity=${meta.table}&action=form&id=${row[meta.pk]}">Xem</a>
  </c:when>
  <c:otherwise>
    <a class="btn" href="?entity=${meta.table}&action=form&id=${row[meta.pk]}">Sửa</a>
  </c:otherwise>
</c:choose>
<c:if test="${meta.table=='HoaDon'}">
    <c:if test="${row.trang_thai=='ĐÃ ĐẶT' || row.trang_thai=='CHƯA THANH TOÁN' || row.trang_thai=='MỚI THANH TOÁN CỌC'}">
        <a class="btn" href="${pageContext.request.contextPath}/admin/invoice-management?op=paymentForm&id=${row.ma_hd}" style="background:#2e7d32;color:white;">Thanh toán</a>
    </c:if>
</c:if>
<form method="post" style="display:inline;" onsubmit="return confirm('Xóa?')">
<input type="hidden" name="entity" value="${meta.table}">
<input type="hidden" name="op" value="delete"><input type="hidden" name="${meta.pk}" value="${row[meta.pk]}">
<button class="btn">Xóa</button></form></td></tr>
</c:forEach>
<c:if test="${empty rows}"><tr><td colspan="20" style="text-align:center;padding:20px;">Chưa có dữ liệu</td></tr>
</c:forEach>
</tbody></table></div>
<p><c:forEach begin="1" end="${totalPages}" var="n">
<a class="btn" href="?entity=${meta.table}&page=${n}&search=${param.search}">${n}</a>
</c:forEach></p>

<%-- Khu vực Voucher áp dụng trên trang Quản lý Sân (loai=2) / Dịch vụ (loai=3) --%>
<c:if test="${meta.table=='San' || meta.table=='DichVu'}">
<hr style="margin:30px 0;border:none;border-top:2px solid #e0e0e0;">
<div class="voucher-section" style="margin-top:10px;">
    <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px;">
        <h3 style="margin:0;">Voucher áp dụng — ${voucherLoaiLabel}</h3>
        <div>
            <a class="btn" href="?entity=Voucher&loai=${voucherLoai}">Xem tất cả</a>
            <a class="btn" href="?entity=Voucher&action=form&loai=${voucherLoai}&back=${meta.table}" style="background:#1565c0;color:#fff;">+ Thêm Voucher</a>
        </div>
    </div>
    <p style="color:#666;font-size:13px;margin:8px 0 12px;">
        Chỉ hiển thị Voucher có <b>loai = ${voucherLoai}</b>
        (<c:choose>
            <c:when test="${voucherLoai==2}">Sân</c:when>
            <c:when test="${voucherLoai==3}">Dịch vụ</c:when>
            <c:otherwise>Hóa đơn</c:otherwise>
        </c:choose>).
        Không hiển thị Voucher của các loại khác.
    </p>
    <div style="overflow-x:auto;">
    <table>
        <thead>
        <tr>
            <th>Mã Voucher</th>
            <th>Mã Code</th>
            <th>Tên Voucher</th>
            <th>Loại giảm</th>
            <th>Giá trị</th>
            <th>Đơn tối thiểu</th>
            <th>Bắt đầu</th>
            <th>Kết thúc</th>
            <th>Số lượng</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${entityVouchers}" var="v">
            <tr>
                <td>${v.ma_voucher}</td>
                <td><strong>${v.ma_code}</strong></td>
                <td>${v.ten_voucher}</td>
                <td>
                    <c:choose>
                        <c:when test="${v.loai_giam=='PERCENT' || v.loai_giam=='GIAM_THEO_PHAN_TRAM'}">%</c:when>
                        <c:otherwise>Tiền</c:otherwise>
                    </c:choose>
                </td>
                <td>${v.gia_tri}<c:if test="${v.loai_giam=='PERCENT' || v.loai_giam=='GIAM_THEO_PHAN_TRAM'}">%</c:if></td>
                <td>${v.don_toi_thieu}</td>
                <td>${v.ngay_bat_dau}</td>
                <td>${v.ngay_ket_thuc}</td>
                <td>${v.so_luong}</td>
                <td>${v.trang_thai}</td>
                <td>
                    <a class="btn" href="?entity=Voucher&action=form&id=${v.ma_voucher}&back=${meta.table}">Sửa</a>
                    <form method="post" style="display:inline;" onsubmit="return confirm('Xóa voucher ${v.ma_code}?')">
                        <input type="hidden" name="entity" value="Voucher">
                        <input type="hidden" name="op" value="delete">
                        <input type="hidden" name="ma_voucher" value="${v.ma_voucher}">
                        <input type="hidden" name="back" value="${meta.table}">
                        <button class="btn" type="submit">Xóa</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty entityVouchers}">
            <tr><td colspan="11" style="text-align:center;padding:16px;color:#888;">Chưa có voucher áp dụng cho ${voucherLoaiLabel}</td></tr>
        </c:if>
        </tbody>
    </table>
    </div>
</div>
</c:if>

</div>
<%@ include file="/jsp/common/footer.jspf" %>
```

---

## 7. CÁC CLASS DÙNG CHUNG

### 7.1 DBConnection.java
**Đường dẫn:** `src/main/java/com/footballbooking/config/DBConnection.java`

**Lý do cần thiết:** Tất cả các Servlet đều cần kết nối Database để thực hiện các thao tác CRUD.

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
 * Có thể ghi đè bằng các biến môi trường DB_URL, DB_USER, DB_PASSWORD. 
 */
public final class DBConnection {
    // Cờ đánh dấu driver đã được tải hay chưa
    private static volatile boolean driverLoaded = false;

    static {
        try {
            // Tải driver JDBC cho SQL Server
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            System.err.println("CẢNH BÁO: Không tìm thấy driver mssql-jdbc. Các tính năng cơ sở dữ liệu sẽ không khả dụng. " + e.getMessage());
        }
    }

    private DBConnection() {}
    
    // Cấu hình thông tin kết nối mặc định hoặc lấy từ biến môi trường
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:sqlserver://localhost:1433;databaseName=QuanLySanBong;encrypt=true;trustServerCertificate=true");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "sa");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "123456");

    /**
     * Khởi tạo và trả về kết nối đến cơ sở dữ liệu SQL Server.
     * @return Connection đối tượng kết nối
     * @throws SQLException nếu có lỗi kết nối hoặc thiếu driver
     */
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            throw new SQLException("Driver SQL Server JDBC không khả dụng. Vui lòng kiểm tra lại cấu hình Maven.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

### 7.2 Entities.java
**Đường dẫn:** `src/main/java/com/footballbooking/model/Entities.java`

**Lý do cần thiết:** Chứa định nghĩa Entity KhachHang và NhanVien.

**Code hoàn chỉnh:**
```java
package com.footballbooking.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Lớp chứa các thực thể (Entities) ánh xạ trực tiếp với các bảng trong cơ sở dữ liệu.
 * Được sử dụng để đóng gói dữ liệu và trao đổi giữa các tầng trong kiến trúc MVC.
 */
public final class Entities {
    private Entities() {}

    /** Thực thể Khách hàng */
    public static class KhachHang {
        public String maKh, hoTen, sdt, email;
    }

    /** Thực thể Nhân viên */
    public static class NhanVien {
        public String maNv, tenNv, sdt, chucVu, matKhau;
    }

    /** Thực thể Sân bóng */
    public static class San {
        public String maSan, tenSan, loaiSan, trangThai;
        public double giaTheoGio;
    }

    /** Thực thể Lịch đặt sân */
    public static class LichSan {
        public String maLich, maSan, buoi, ca, gioBatDau, gioKetThuc, trangThai;
        public LocalDate ngayDat;
        public int soCa;
    }

    /** Thực thể Dịch vụ đi kèm */
    public static class DichVu {
        public String maDv, tenDv;
        public double donGia;
    }

    /**
     * Hằng số loại Voucher — mở rộng thêm loại mới chỉ cần bổ sung hằng số.
     * loai = 1 → Hóa đơn (Booking)
     * loai = 2 → Sân
     * loai = 3 → Dịch vụ
     */
    public static final class VoucherLoai {
        public static final int HOA_DON = 1;
        public static final int SAN = 2;
        public static final int DICH_VU = 3;

        private VoucherLoai() {}

        public static String ten(int loai) {
            return switch (loai) {
                case HOA_DON -> "Hóa đơn";
                case SAN -> "Sân";
                case DICH_VU -> "Dịch vụ";
                default -> "Khác (" + loai + ")";
            };
        }

        public static boolean isValid(int loai) {
            return loai == HOA_DON || loai == SAN || loai == DICH_VU;
        }
    }

    /**
     * Thực thể Voucher — ánh xạ bảng Voucher.
     * loai_giam: MONEY (giảm tiền) hoặc PERCENT (giảm %).
     */
    public static class Voucher {
        private String maVoucher;
        private String maCode;
        private String tenVoucher;
        private String loaiGiam;
        private double giaTri;
        private double donToiThieu;
        private LocalDate ngayBatDau;
        private LocalDate ngayKetThuc;
        private int soLuong;
        private String trangThai;
        private int loai;

        public Voucher() {}

        public Voucher(String maVoucher, String maCode, String tenVoucher, String loaiGiam,
                       double giaTri, double donToiThieu, LocalDate ngayBatDau, LocalDate ngayKetThuc,
                       int soLuong, String trangThai, int loai) {
            this.maVoucher = maVoucher;
            this.maCode = maCode;
            this.tenVoucher = tenVoucher;
            this.loaiGiam = loaiGiam;
            this.giaTri = giaTri;
            this.donToiThieu = donToiThieu;
            this.ngayBatDau = ngayBatDau;
            this.ngayKetThuc = ngayKetThuc;
            this.soLuong = soLuong;
            this.trangThai = trangThai;
            this.loai = loai;
        }

        public String getMaVoucher() { return maVoucher; }
        public void setMaVoucher(String maVoucher) { this.maVoucher = maVoucher; }

        public String getMaCode() { return maCode; }
        public void setMaCode(String maCode) { this.maCode = maCode; }

        public String getTenVoucher() { return tenVoucher; }
        public void setTenVoucher(String tenVoucher) { this.tenVoucher = tenVoucher; }

        public String getLoaiGiam() { return loaiGiam; }
        public void setLoaiGiam(String loaiGiam) { this.loaiGiam = loaiGiam; }

        public double getGiaTri() { return giaTri; }
        public void setGiaTri(double giaTri) { this.giaTri = giaTri; }

        public double getDonToiThieu() { return donToiThieu; }
        public void setDonToiThieu(double donToiThieu) { this.donToiThieu = donToiThieu; }

        public LocalDate getNgayBatDau() { return ngayBatDau; }
        public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }

        public LocalDate getNgayKetThuc() { return ngayKetThuc; }
        public void setNgayKetThuc(LocalDate ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

        public int getSoLuong() { return soLuong; }
        public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

        public String getTrangThai() { return trangThai; }
        public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

        public int getLoai() { return loai; }
        public void setLoai(int loai) { this.loai = loai; }

        /** true nếu giảm theo phần trăm */
        public boolean isPercent() {
            return "PERCENT".equalsIgnoreCase(loaiGiam);
        }

        /** true nếu giảm số tiền cố định */
        public boolean isMoney() {
            return "MONEY".equalsIgnoreCase(loaiGiam);
        }

        /** Nhãn loại áp dụng (Hóa đơn / Sân / Dịch vụ) */
        public String getTenLoai() {
            return VoucherLoai.ten(loai);
        }

        @Override
        public String toString() {
            return "Voucher{" +
                    "maVoucher='" + maVoucher + '\'' +
                    ", maCode='" + maCode + '\'' +
                    ", tenVoucher='" + tenVoucher + '\'' +
                    ", loaiGiam='" + loaiGiam + '\'' +
                    ", giaTri=" + giaTri +
                    ", donToiThieu=" + donToiThieu +
                    ", ngayBatDau=" + ngayBatDau +
                    ", ngayKetThuc=" + ngayKetThuc +
                    ", soLuong=" + soLuong +
                    ", trangThai='" + trangThai + '\'' +
                    ", loai=" + loai +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Voucher voucher = (Voucher) o;
            return Objects.equals(maVoucher, voucher.maVoucher);
        }

        @Override
        public int hashCode() {
            return Objects.hash(maVoucher);
        }
    }

    /** Thực thể Hóa đơn */
    public static class HoaDon {
        public String maHd, maKh, maNv, trangThai, maVoucher;
        public double tongTien, tienCoc, soTienConLai, tienGiam;
    }

    /** Thực thể Chi tiết hóa đơn */
    public static class CTHD {
        public int maCthd;
        public String maHd, maLich, maDv;
        public int soLuong;
        public double donGia, thanhTien;
    }
}
```

---

## 8. CẤU TRÚC DATABASE

### 8.1 File SQL
**Đường dẫn:** `database/football_booking.sql`

**Code hoàn chỉnh:**
```sql
/* ============================================================
   Sân Bóng Hoa Sen — Script khởi tạo SQL Server
   Chạy file này trong SSMS trước khi deploy ứng dụng.
   ============================================================ */
IF DB_ID('QuanLySanBong') IS NULL
    CREATE DATABASE QuanLySanBong;
GO
USE QuanLySanBong;
GO

/* Xóa bảng cũ nếu cần tạo lại từ đầu (bỏ comment khi dev) */
/*
DROP TABLE IF EXISTS CTHD;
DROP TABLE IF EXISTS HoaDon;
DROP TABLE IF EXISTS LichSan;
DROP TABLE IF EXISTS DichVu;
DROP TABLE IF EXISTS KhachHang;
DROP TABLE IF EXISTS NhanVien;
DROP TABLE IF EXISTS San;
*/

IF OBJECT_ID('CTHD', 'U') IS NULL
BEGIN
    CREATE TABLE San (
        ma_san      VARCHAR(10)  PRIMARY KEY,
        ten_san     NVARCHAR(100) NOT NULL,
        loai_san    NVARCHAR(30)  NOT NULL,
        gia_theo_gio DECIMAL(18,2) NOT NULL CHECK (gia_theo_gio >= 0),
        trang_thai  NVARCHAR(20)  NOT NULL DEFAULT N'HOẠT ĐỘNG'
    );

    CREATE TABLE KhachHang (
        ma_kh   VARCHAR(10) PRIMARY KEY,
        ho_ten  NVARCHAR(100) NOT NULL,
        sdt     VARCHAR(15) NOT NULL UNIQUE,
        email   VARCHAR(100) NULL
    );

    CREATE TABLE NhanVien (
        ma_nv    VARCHAR(10) PRIMARY KEY,
        ten_nv   NVARCHAR(100) NOT NULL,
        sdt      VARCHAR(15) NOT NULL UNIQUE,
        chuc_vu  NVARCHAR(50) NOT NULL,
        mat_khau NVARCHAR(100) NOT NULL DEFAULT N'admin123'
    );

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

    CREATE TABLE DichVu (
        ma_dv   VARCHAR(10) PRIMARY KEY,
        ten_dv  NVARCHAR(100) NOT NULL,
        don_gia DECIMAL(18,2) NOT NULL CHECK (don_gia >= 0)
    );

    CREATE TABLE HoaDon (
        ma_hd           VARCHAR(10) PRIMARY KEY,
        ma_kh           VARCHAR(10) NOT NULL REFERENCES KhachHang(ma_kh),
        ma_nv           VARCHAR(10) NULL REFERENCES NhanVien(ma_nv),
        ngay_lap        DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        tong_tien       DECIMAL(18,2) NOT NULL,
        tien_coc        DECIMAL(18,2) NOT NULL DEFAULT 200000,
        so_tien_con_lai DECIMAL(18,2) NOT NULL,
        han_thanh_toan  DATETIME NOT NULL,
        trang_thai      NVARCHAR(30) NOT NULL DEFAULT N'CHƯA THANH TOÁN'
    );

    CREATE TABLE CTHD (
        ma_cthd    INT IDENTITY PRIMARY KEY,
        ma_hd      VARCHAR(10) NOT NULL REFERENCES HoaDon(ma_hd),
        ma_lich    VARCHAR(10) NOT NULL REFERENCES LichSan(ma_lich),
        ma_dv      VARCHAR(10) NULL REFERENCES DichVu(ma_dv),
        so_luong   INT NOT NULL DEFAULT 1,
        don_gia    DECIMAL(18,2) NOT NULL,
        thanh_tien DECIMAL(18,2) NOT NULL
    );

    /* --- Dữ liệu mẫu: 4 sân --- */
    INSERT INTO San (ma_san, ten_san, loai_san, gia_theo_gio, trang_thai) VALUES
    ('S01', N'Sân 1',    N'Sân 7 người',  600000, N'HOẠT ĐỘNG'),
    ('S02', N'Sân 2',    N'Sân 7 người',  600000, N'HOẠT ĐỘNG'),
    ('S03', N'Sân 3',    N'Sân 7 người',  600000, N'HOẠT ĐỘNG'),
    ('S04', N'Sân 4',    N'Sân 7 người',  600000, N'HOẠT ĐỘNG');

    INSERT INTO NhanVien (ma_nv, ten_nv, sdt, chuc_vu, mat_khau) VALUES
    ('NV001', N'Quản trị viên', '0900000000', N'Admin', N'admin123'),
    ('NV002', N'Quản trị viên 2', '0365365113', N'Admin', N'1');

    INSERT INTO DichVu (ma_dv, ten_dv, don_gia) VALUES
    ('DV001', N'Nước suối', 10000),
    ('DV002', N'Áo bib', 30000),
    ('DV003', N'Bóng thi đấu', 50000);
END
GO

/* Migration cho DB đã tồn tại */
IF COL_LENGTH('LichSan', 'buoi') IS NULL ALTER TABLE LichSan ADD buoi NVARCHAR(20) NULL;
IF COL_LENGTH('LichSan', 'ngay_dat') IS NULL ALTER TABLE LichSan ADD ngay_dat DATE NULL;
IF COL_LENGTH('LichSan', 'so_ca') IS NULL ALTER TABLE LichSan ADD so_ca TINYINT NULL;
IF COL_LENGTH('HoaDon', 'han_thanh_toan') IS NULL ALTER TABLE HoaDon ADD han_thanh_toan DATETIME NULL;
IF COL_LENGTH('HoaDon', 'tien_coc') IS NULL ALTER TABLE HoaDon ADD tien_coc DECIMAL(18,2) NOT NULL DEFAULT 200000;
IF COL_LENGTH('HoaDon', 'so_tien_con_lai') IS NULL ALTER TABLE HoaDon ADD so_tien_con_lai DECIMAL(18,2) NOT NULL DEFAULT 0;
IF COL_LENGTH('NhanVien', 'mat_khau') IS NULL ALTER TABLE NhanVien ADD mat_khau NVARCHAR(100) NOT NULL DEFAULT N'admin123';
GO

/* --- DỮ LIỆU LỊCH SÂN CHO THỨ 7 (04/07/2026) VÀ CHỦ NHẬT (05/07/2026) --- */

-- Thứ 6 - 24/07/2026 (Sân S02 tương ứng Thứ 6 - Ca 14h-16h đặc biệt)
INSERT INTO LichSan (ma_lich, ma_san, ngay_dat, buoi, ca, so_ca, gio_bat_dau, gio_ket_thuc, trang_thai) VALUES
('L021', 'S02', '2026-07-24', N'Sáng',   N'07:00 - 08:30', 1, '2026-07-24 07:00', '2026-07-24 08:30', N'CÒN TRỐNG'),
('L022', 'S02', '2026-07-24', N'Sáng',   N'09:00 - 10:30', 2, '2026-07-24 09:00', '2026-07-24 10:30', N'ĐÃ ĐẶT'),
('L023', 'S02', '2026-07-24', N'Chiều',  N'13:00 - 14:30', 4, '2026-07-24 13:00', '2026-07-24 14:30', N'CÒN TRỐNG'),
('L024', 'S02', '2026-07-24', N'Chiều',  N'14:00 - 16:00', 5, '2026-07-24 14:00', '2026-07-24 16:00', N'ĐÃ ĐẶT'),
('L025', 'S02', '2026-07-24', N'Chiều',  N'16:30 - 18:00', 6, '2026-07-24 16:30', '2026-07-24 18:00', N'CÒN TRỐNG'),
('L026', 'S02', '2026-07-24', N'Tối',    N'18:00 - 19:30', 7, '2026-07-24 18:00', '2026-07-24 19:30', N'ĐANG GIỮ CHỖ'),
('L027', 'S02', '2026-07-24', N'Tối',    N'20:00 - 22:00', 8, '2026-07-24 20:00', '2026-07-24 22:00', N'CÒN TRỐNG'),
('L028', 'S02', '2026-07-24', N'Tối',    N'22:00 - 23:00', 9, '2026-07-24 22:00', '2026-07-24 23:00', N'CÒN TRỐNG');

-- Thứ 7 - 04/07/2026 (Sân S03 tương ứng Thứ 7)
INSERT INTO LichSan (ma_lich, ma_san, ngay_dat, buoi, ca, so_ca, gio_bat_dau, gio_ket_thuc, trang_thai) VALUES
('L011', 'S03', '2026-07-04', N'Sáng',   N'07:00 - 08:30', 1, '2026-07-04 07:00', '2026-07-04 08:30', N'CÒN TRỐNG'),
('L012', 'S03', '2026-07-04', N'Sáng',   N'09:00 - 10:30', 2, '2026-07-04 09:00', '2026-07-04 10:30', N'ĐÃ ĐẶT'),
('L013', 'S03', '2026-07-04', N'Chiều',  N'15:00 - 16:30', 5, '2026-07-04 15:00', '2026-07-04 16:30', N'ĐANG GIỮ CHỖ'),
('L014', 'S03', '2026-07-04', N'Tối',    N'18:00 - 19:30', 7, '2026-07-04 18:00', '2026-07-04 19:30', N'CÒN TRỐNG'),
('L015', 'S03', '2026-07-04', N'Tối',    N'20:00 - 21:30', 8, '2026-07-04 20:00', '2026-07-04 21:30', N'CÒN TRỐNG');

-- Chủ nhật - 05/07/2026 (Sân S04 tương ứng Chủ nhật)
INSERT INTO LichSan (ma_lich, ma_san, ngay_dat, buoi, ca, so_ca, gio_bat_dau, gio_ket_thuc, trang_thai) VALUES
('L016', 'S04', '2026-07-05', N'Sáng',   N'07:00 - 08:30', 1, '2026-07-05 07:00', '2026-07-05 08:30', N'CÒN TRỐNG'),
('L017', 'S04', '2026-07-05', N'Sáng',   N'09:00 - 10:30', 2, '2026-07-05 09:00', '2026-07-05 10:30', N'CÒN TRỐNG'),
('L018', 'S04', '2026-07-05', N'Chiều',  N'15:00 - 16:30', 5, '2026-07-05 15:00', '2026-07-05 16:30', N'ĐÃ ĐẶT'),
('L019', 'S04', '2026-07-05', N'Tối',    N'18:00 - 19:30', 7, '2026-07-05 18:00', '2026-07-05 19:30', N'ĐÃ ĐẶT'),
('L020', 'S04', '2026-07-05', N'Tối',    N'20:00 - 21:30', 8, '2026-07-05 20:00', '2026-07-05 21:30', N'ĐANG GIỮ CHỖ');

PRINT N'CSDL QuanLySanBong sẵn sàng.';
GO
```

---

## 9. LUỒNG XỬ LÝ

### 9.1 Luồng đăng nhập Admin

```
Người dùng truy cập /admin/login
        ↓
AdminLoginServlet.doGet()
        ↓
Kiểm tra Session (đã đăng nhập chưa?)
        ↓
Có → Chuyển đến /admin (Dashboard)
Không → Thực hiện tự động đăng nhập với tài khoản demo
        ↓
Gọi authenticate() để kiểm tra trong DB
        ↓
Truy vấn SQL: SELECT ma_nv, ten_nv FROM NhanVien WHERE sdt=? AND mat_khau=?
        ↓
Tìm thấy → Lưu vào Session (adminUser, adminId)
        ↓
Chuyển đến /admin (Dashboard)
Không tìm thấy → Dùng tài khoản mặc định
```

### 9.2 Luồng đăng xuất

```
Người dùng nhấn "Đăng xuất"
        ↓
Truy cập /admin/logout
        ↓
AdminLogoutServlet.doGet()
        ↓
Hủy Session (invalidate)
        ↓
Chuyển về /admin/login
```

### 9.3 Luồng phân quyền

```
Truy cập /admin/*
        ↓
AdminAuthFilter.doFilter()
        ↓
Kiểm tra Session có adminUser không
        ↓
Có → Cho phép truy cập
Không → Chuyển về /admin/login
```

### 9.4 Luồng CRUD Khách hàng/Nhân viên

```
Truy cập /admin/manage?entity=KhachHang
        ↓
AdminAuthFilter (kiểm tra đăng nhập)
        ↓
AdminCrudServlet.doGet()
        ↓
Lấy danh sách từ DB
        ↓
Hiển thị list.jsp
        ↓
Người dùng nhấn "Thêm mới"
        ↓
Truy cập /admin/manage?entity=KhachHang&action=form
        ↓
Hiển thị form.jsp (trống)
        ↓
Người dùng điền thông tin và nhấn "Lưu"
        ↓
Gửi POST đến /admin/manage
        ↓
AdminCrudServlet.doPost()
        ↓
Thực hiện INSERT vào DB
        ↓
Chuyển về danh sách với thông báo thành công
```

---

## 10. CODE ĐẦY ĐỦ

### 10.1 Tất cả file do Người 1 phụ trách

**Danh sách:**
1. `AdminLoginServlet.java` - Đăng nhập Admin (76 dòng)
2. `AdminLogoutServlet.java` - Đăng xuất Admin (25 dòng)
3. `AdminAuthFilter.java` - Bộ lọc phân quyền (40 dòng)
4. `login.jsp` - Trang đăng nhập (21 dòng)
5. `dashboard.jsp` - Trang chủ Admin (40 dòng)
6. `form.jsp` - Form thêm/sửa (202 dòng)
7. `list.jsp` - Danh sách (124 dòng)

### 10.2 File có thể cần sửa (FILE DÙNG CHUNG)

**AdminCrudServlet.java** (959 dòng)
- **Lý do:** Đây là Servlet tổng quát xử lý CRUD cho tất cả các bảng
- **Việc cần làm:** Chỉ sửa phần liên quan đến `KhachHang` và `NhanVien`
- **Lưu ý:** Không sửa phần xử lý San, DichVu, HoaDon, Voucher, CTHD, LichSan

**Entities.java** (196 dòng)
- **Lý do:** Chứa định nghĩa Entity KhachHang và NhanVien
- **Việc cần làm:** Có thể bổ sung getter/setter nếu cần

**header.jspf** (15 dòng) và **footer.jspf** (4 dòng)
- Có thể thêm nội dung vào header/footer
- Không xóa nội dung do người khác tạo

---

## 11. GIẢI THÍCH TỪNG PHƯƠNG THỨC QUAN TRỌNG

### 11.1 AdminLoginServlet.authenticate()

```java
private boolean authenticate(HttpServletRequest req, String phone, String password) throws SQLException
```

**Mục đích:** Kiểm tra thông tin đăng nhập của Admin/Nhân viên

**Tham số:**
- `req`: Request chứa thông tin đăng nhập
- `phone`: Số điện thoại
- `password`: Mật khẩu

**Return:**
- `true`: Đăng nhập thành công
- `false`: Đăng nhập thất bại

**Cách hoạt động:**
1. Mở kết nối DB
2. Truy vấn SQL: `SELECT ma_nv, ten_nv FROM NhanVien WHERE sdt=? AND mat_khau=?`
3. Nếu tìm thấy → Lưu vào Session và trả về true
4. Nếu không → Trả về false

**Lưu ý:** Hiện tại mật khẩu lưu plain text. Nếu cần bảo mật, có thể mã hóa bằng BCrypt.

### 11.2 AdminLogoutServlet.doGet()

```java
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
```

**Mục đích:** Đăng xuất và hủy Session

**Cách hoạt động:**
1. Lấy Session hiện tại (không tạo mới)
2. Gọi `session.invalidate()` để hủy
3. Chuyển về trang đăng nhập

### 11.3 AdminAuthFilter.doFilter()

```java
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
```

**Mục đích:** Kiểm tra quyền truy cập khu vực Admin

**Cách hoạt động:**
1. Lấy Session hiện tại
2. Kiểm tra thuộc tính `adminUser`
3. Nếu không có → Chuyển về `/admin/login`
4. Nếu có → Cho phép tiếp tục

---

## 12. CÁCH TEST CHỨC NĂNG

### 12.1 Test đăng nhập

**Cách 1: Sử dụng trình duyệt**
```
1. Mở trình duyệt, truy cập: http://localhost:8080/QuanLySanBong/admin/login
2. Hệ thống tự động đăng nhập với tài khoản demo
3. Kiểm tra xem có chuyển đến Dashboard không
```

**Cách 2: Test với tài khoản khác**
```
1. Truy cập: http://localhost:8080/QuanLySanBong/admin/login?phone=0900000000&password=admin123
2. Kiểm tra đăng nhập thành công
3. Test nhập sai password → Kiểm tra hiển thị lỗi
```

**Kiểm tra Session:**
```
1. Sau khi đăng nhập, mở Developer Tools (F12)
2. Vào tab Application → Cookies
3. Kiểm tra có Session ID không
4. Kiểm tra giá trị adminUser, adminId
```

### 12.2 Test đăng xuất

```
1. Đăng nhập vào Admin
2. Nhấn nút "Đăng xuất"
3. Kiểm tra chuyển về trang login
4. Thử truy cập lại /admin → Phải chuyển về /admin/login
```

### 12.3 Test CRUD Khách hàng

```
1. Đăng nhập Admin
2. Truy cập: http://localhost:8080/QuanLySanBong/admin/manage?entity=KhachHang
3. Nhấn "Thêm mới"
4. Điền thông tin:
   - Họ tên: Nguyễn Văn A
   - SĐT: 0123456789
   - Email: test@example.com
5. Nhấn "Lưu"
6. Kiểm tra khách hàng mới xuất hiện trong danh sách
7. Nhấn "Sửa" → Cập nhật thông tin → Lưu
8. Nhấn "Xóa" → Xác nhận → Kiểm tra đã xóa
```

### 12.4 Test CRUD Nhân viên

```
1. Truy cập: http://localhost:8080/QuanLySanBong/admin/manage?entity=NhanVien
2. Thêm nhân viên mới:
   - Tên: Nhân viên test
   - SĐT: 0999888777
   - Chức vụ: Staff
   - Mật khẩu: test123
3. Lưu và kiểm tra
4. Sửa thông tin và đổi mật khẩu
5. Xác nhận mật khẩu mới hoạt động
```

### 12.5 Test phân quyền

```
1. Đăng xuất khỏi Admin
2. Truy cập trực tiếp: http://localhost:8080/QuanLySanBong/admin
3. Kiểm tra tự động chuyển về /admin/login
4. Đăng nhập lại → Kiểm tra truy cập được
```

---

## 13. CÁC LỖI THƯỜNG GẶP

### 13.1 Lỗi đăng nhập thất bại

**Triệu chứng:** Nhập đúng SĐT và MK nhưng không đăng nhập được

**Nguyên nhân:**
- Sai số điện thoại hoặc mật khẩu
- Database chưa có dữ liệu mẫu
- Lỗi kết nối DB

**Cách sửa:**
```sql
-- Kiểm tra dữ liệu
SELECT * FROM NhanVien;

-- Thêm dữ liệu mẫu nếu chưa có
INSERT INTO NhanVien (ma_nv, ten_nv, sdt, chuc_vu, mat_khau) VALUES
('NV001', N'Quản trị viên', '0900000000', N'Admin', N'admin123');
```

### 13.2 Lỗi Session bị mất

**Triệu chứng:** Đăng nhập được nhưng sau đó bị chuyển về login

**Nguyên nhân:**
- Session timeout (mặc định 30 phút)
- Xóa Cookie trình duyệt
- Server restart

**Cách sửa:**
- Tăng thời gian Session: `session.setMaxInactiveInterval(3600);`
- Kiểm tra cấu hình Tomcat

### 13.3 Lỗi không tìm thấy trang login.jsp

**Triệu chứng:** 404 khi truy cập /admin/login

**Nguyên nhân:**
- File login.jsp chưa được tạo
- Đường dẫn sai

**Cách sửa:**
- Kiểm tra file tồn tại tại: `src/main/webapp/jsp/admin/login.jsp`
- Kiểm tra cấu hình trong web.xml hoặc @WebServlet annotation

### 13.4 Lỗi AdminAuthFilter chặn truy cập

**Triệu chứng:** Đã đăng nhập nhưng bị chuyển về login

**Nguyên nhân:**
- Session attribute có tên khác `adminUser`
- Filter chưa được đăng ký đúng cách

**Cách sửa:**
```java
// Kiểm tra lại tên attribute trong Filter
if (session == null || session.getAttribute("adminUser") == null) {
    // Chuyển về login
}

// Đảm bảo khi đăng nhập, lưu đúng tên:
session.setAttribute("adminUser", tenNv); // Đúng
session.setAttribute("adminId", maNv);    // Đúng
```

---

## 14. LƯU Ý QUAN TRỌNG

### 14.1 FILE DÙNG CHUNG

Các file sau là FILE DÙNG CHUNG, không tự ý sửa toàn bộ:

1. **AdminCrudServlet.java** (959 dòng)
   - Người 1 chỉ sửa phần xử lý `KhachHang` và `NhanVien`
   - Không sửa phần xử lý San, DichVu, HoaDon, Voucher, CTHD, LichSan

2. **Entities.java** (196 dòng)
   - Chỉ thêm/sửa Entity `KhachHang` và `NhanVien`
   - Không sửa các Entity khác

3. **login.jsp, dashboard.jsp, form.jsp, list.jsp**
   - Có thể thêm nội dung
   - Không xóa nội dung do người khác tạo

### 14.2 QUY TẮC CODE

1. **Mỗi file phải có comment đầu mô tả:**
```java
/**
 * Servlet xử lý đăng nhập cho quản trị viên.
 * Người phụ trách: Người 1
 * Chức năng: Xác thực và quản lý Session Admin
 */
```

2. **Code phải đơn giản, dễ đọc:**
   - Không dùng Design Pattern phức tạp
   - Comment rõ ràng từng bước
   - Tên biến, phương thức rõ nghĩa

3. **Không dùng JavaScript xử lý nghiệp vụ:**
   - Chỉ dùng HTML form gửi request đến Servlet
   - Toàn bộ xử lý phải làm ở Server-side

4. **Tuân thủ MVC:**
   - Entity: Định nghĩa cấu trúc dữ liệu
   - Repository: Truy cập DB (có thể bỏ qua nếu đơn giản)
   - Service: Xử lý nghiệp vụ (có thể bỏ qua nếu đơn giản)
   - Servlet: Xử lý request/response
   - JSP: Hiển thị giao diện

### 14.3 PHÂN QUYỀN

**Hiện tại chưa có phân quyền chi tiết**, tất cả Admin đều có quyền truy cập toàn bộ.

**Gợi ý mở rộng (không bắt buộc):**
```java
// Trong AdminAuthFilter
String chucVu = (String) session.getAttribute("adminChucVu");
if ("Admin".equals(chucVu)) {
    // Cho phép tất cả
} else if ("Staff".equals(chucVu)) {
    // Chỉ cho phép xem, không cho phép xóa
    if ("delete".equals(op)) {
        // Chặn
    }
}
```

---

## 15. TÀI LIỆU THAM KHẢO

- **Java Servlet:** https://docs.oracle.com/javaee/8/tutorial/servlets.htm
- **Session:** https://docs.oracle.com/javaee/8/tutorial/servlets012.htm
- **Filter:** https://docs.oracle.com/javaee/8/tutorial/servlets013.htm
- **JDBC:** https://docs.oracle.com/javase/tutorial/jdbc/

---

**NGƯỜI PHỤ TRÁCH:** Người 1  
**FILE ĐẦU RA:** 01_NguoiDung_DangNhap.md  
**TRẠNG THÁI:** Hoàn thành