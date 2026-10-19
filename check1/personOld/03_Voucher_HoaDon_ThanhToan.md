# 03_VOUCHER_HOA DON_THANH TOAN.md
## PHẦN 3 — NGƯỜI 3: VOUCHER + HÓA ĐƠN + THANH TOÁN

---

## 1. CHỨC NĂNG PHỤ TRÁCH

Người 3 phụ trách toàn bộ chức năng liên quan đến:

- Quản lý Voucher (CRUD)
- Áp dụng Voucher cho hóa đơn
- Tính tiền (tiền sân, tiền dịch vụ, tiền giảm, tiền cọc, tiền còn lại)
- Quản lý hóa đơn
- Thanh toán (đặt cọc, thanh toán phần còn lại)

**Lưu ý quan trọng:**
- Không code chức năng đăng nhập, đặt sân, quản lý sân, hay CronJob
- Chỉ làm việc trên các file liên quan đến Voucher, Hóa đơn, Thanh toán
- Nếu cần sửa file của người khác, phải đánh dấu "FILE DÙNG CHUNG"

---

## 2. CẤU TRÚC THƯ MỤC

```
src/main/java/com/footballbooking/
├── config/
│   ├── DBConnection.java                [CLASS DÙNG CHUNG]
│   └── ExpiryListener.java              [CLASS DÙNG CHUNG]
├── controller/
│   ├── InvoiceServlet.java              (Xem hóa đơn)
│   ├── InvoiceManagementServlet.java    (Quản lý thanh toán)
│   ├── AdminCrudServlet.java            [CLASS DÙNG CHUNG]
│   └── ... (các Servlet khác do người khác phụ trách)
├── model/
│   └── Entities.java                    [CLASS DÙNG CHUNG]
├── repository/
│   ├── BookingRepository.java           [CLASS DÙNG CHUNG]
│   └── VoucherRepository.java           (Quản lý Voucher)
├── service/
│   ├── BookingService.java              [CLASS DÙNG CHUNG]
│   └── VoucherService.java              (Xử lý nghiệp vụ Voucher)
└── utils/
    ├── DbConnection.java                [CLASS DÙNG CHUNG]
    ├── JsonUtil.java                    [CLASS DÙNG CHUNG]
    └── Slots.java                       [CLASS DÙNG CHUNG]

src/main/webapp/jsp/
├── user/
│   └── invoice.jsp                     (Xem hóa đơn)
└── admin/
    └── invoice-payment.jsp             (Form thanh toán)

database/
└── football_booking.sql                [CLASS DÙNG CHUNG]
```

---

## 3. DATABASE LIÊN QUAN

### 3.1 Bảng Voucher
```sql
CREATE TABLE Voucher (
    ma_voucher VARCHAR(20) PRIMARY KEY,
    ma_code VARCHAR(50) NOT NULL,
    ten_voucher NVARCHAR(100) NOT NULL,
    loai_giam VARCHAR(20) NOT NULL,  -- MONEY hoặc PERCENT
    gia_tri FLOAT NOT NULL DEFAULT 0,
    don_toi_thieu FLOAT NOT NULL DEFAULT 0,
    ngay_bat_dau DATE NOT NULL,
    ngay_ket_thuc DATE NOT NULL,
    so_luong INT NOT NULL DEFAULT 0,
    trang_thai NVARCHAR(30) NOT NULL DEFAULT N'HOẠT ĐỘNG',
    loai INT NOT NULL DEFAULT 1  -- 1=Hóa đơn, 2=Sân, 3=Dịch vụ
);
```

**Giải thích:**
- `loai_giam`: MONEY (giảm tiền cố định) hoặc PERCENT (giảm phần trăm)
- `loai`: 1 - Áp dụng cho Hóa đơn, 2 - Áp dụng cho Sân, 3 - Áp dụng cho Dịch vụ
- `so_luong`: Số lượng voucher còn lại

### 3.2 Bảng HoaDon (Hóa đơn)
```sql
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
```

**Trạng thái hóa đơn:**
- `CHƯA THANH TOÁN` - Mới tạo, chưa thanh toán
- `ĐANG XỬ LÝ` - Đang xử lý
- `MỚI THANH TOÁN CỌC` - Đã thanh toán tiền cọc (30%)
- `ĐÃ THANH TOÁN` - Đã thanh toán toàn bộ
- `ĐÃ HỦY` - Hóa đơn bị hủy
- `ĐÃ XÓA` - Hóa đơn bị xóa (mềm)

### 3.3 Bảng CTHD (Chi tiết hóa đơn)
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

### 4.1 Entity Voucher
```java
public static class Voucher {
    private String maVoucher;
    private String maCode;
    private String tenVoucher;
    private String loaiGiam;      // MONEY hoặc PERCENT
    private double giaTri;        // Giá trị giảm
    private double donToiThieu;   // Đơn hàng tối thiểu
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private int soLuong;          // Số lượng còn lại
    private String trangThai;
    private int loai;             // 1=Hóa đơn, 2=Sân, 3=Dịch vụ
}
```

**Giải thích:**
- `maVoucher`: Mã voucher (VD: VC001)
- `maCode`: Mã code để áp dụng (VD: MORNING10)
- `loaiGiam`: MONEY (giảm tiền) hoặc PERCENT (giảm %)
- `giaTri`: Giá trị giảm (tiền hoặc phần trăm)
- `donToiThieu`: Đơn hàng tối thiểu để áp dụng
- `ngayBatDau`, `ngayKetThuc`: Thời gian hiệu lực
- `soLuong`: Số lượng voucher còn lại
- `trangThai`: HOẠT ĐỘNG, NGỪNG...
- `loai`: 1 - Hóa đơn, 2 - Sân, 3 - Dịch vụ

### 4.2 Entity HoaDon
```java
public static class HoaDon {
    public String maHd, maKh, maNv, trangThai, maVoucher;
    public double tongTien, tienCoc, soTienConLai, tienGiam;
}
```

**Giải thích:**
- `maHd`: Mã hóa đơn (VD: HD000001)
- `maKh`: Mã khách hàng
- `maNv`: Mã nhân viên (tạo hóa đơn)
- `trangThai`: Trạng thái hóa đơn
- `maVoucher`: Mã voucher áp dụng (nếu có)
- `tongTien`: Tổng tiền (sau khi giảm giá)
- `tienCoc`: Tiền cọc (30% tổng tiền)
- `soTienConLai`: Số tiền còn lại cần thanh toán
- `tienGiam`: Tiền giảm giá

### 4.3 Entity CTHD (Chi tiết hóa đơn)
```java
public static class CTHD {
    public int maCthd;
    public String maHd, maLich, maDv;
    public int soLuong;
    public double donGia, thanhTien;
}
```

**Giải thích:**
- `maCthd`: Mã chi tiết hóa đơn (tự tăng)
- `maHd`: Mã hóa đơn
- `maLich`: Mã lịch sân (nếu là tiền sân)
- `maDv`: Mã dịch vụ (nếu là tiền dịch vụ)
- `soLuong`: Số lượng
- `donGia`: Đơn giá
- `thanhTien`: Thành tiền (soLuong * donGia)

---

## 5. CÁC SERVLET

### 5.1 InvoiceServlet.java
**Đường dẫn:** `/invoice`
**Chức năng:** Hiển thị chi tiết hóa đơn cho người dùng

**Code hoàn chỉnh:**
```java
package com.footballbooking.controller;

import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.util.*;

/**
 * Servlet hiển thị chi tiết hóa đơn cho người dùng.
 * Cho phép khách hàng xem lại đơn đặt sân, thực hiện đặt cọc hoặc hủy đơn.
 */
@WebServlet("/invoice")
public class InvoiceServlet extends HttpServlet {
    private final BookingService service = new BookingService();

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        String id = q.getParameter("id");
        try {
            // Tải thông tin chi tiết hóa đơn từ cơ sở dữ liệu
            Map<String, Object> inv = service.invoice(id);
            q.setAttribute("invoice", inv);
            if (inv != null) {
                q.setAttribute("vouchers", service.getAvailableVouchers(id));
            }
        } catch (Exception e) {
            // Hiển thị dữ liệu giả định nếu có lỗi CSDL (phục vụ mục đích demo/kiểm thử)
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma_hd", id); m.put("ho_ten", "Khách hàng"); m.put("sdt", "0900000000");
            m.put("trang_thai", "CHƯA THANH TOÁN");
            m.put("tong_tien", 400000); m.put("tien_coc", 120000); m.put("so_tien_con_lai", 280000);
            m.put("han_thanh_toan", java.time.LocalDateTime.now().plusHours(1).toString());
            List<Map<String, Object>> slots = new ArrayList<>();
            Map<String, Object> sl = new LinkedHashMap<>();
            sl.put("ten_san", "Sân 1"); sl.put("loai_san", "Sân 7 người");
            sl.put("ngay_dat", java.time.LocalDate.now().toString());
            sl.put("gio_bat_dau", "06:00"); sl.put("gio_ket_thuc", "08:00");
            sl.put("thanh_tien", 400000);
            slots.add(sl);
            m.put("slots", slots);
            m.put("services", new ArrayList<>());
            q.setAttribute("invoice", m);
        }
        q.getRequestDispatcher("/jsp/user/invoice.jsp").forward(q, p);
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws IOException {
        String op = q.getParameter("op");
        String id = q.getParameter("id");
        try {
            // Xử lý hành động Hủy hóa đơn
            if ("cancel".equals(op)) {
                service.cancelInvoice(id);
                p.sendRedirect(q.getContextPath() + "/home");
                return;
            }
            // Xử lý hành động Thanh toán đặt cọc (30%)
            if ("pay".equals(op)) {
                service.payDeposit(id);
                p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
                return;
            }
            // Xử lý hành động Thanh toán phần còn lại (70%)
            if ("payRemaining".equals(op)) {
                service.payRemaining(id);
                p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
                return;
            }
            // Xử lý áp dụng Voucher
            if ("applyVoucher".equals(op)) {
                String voucherId = q.getParameter("voucherId");
                service.applyVoucher(id, voucherId);
                p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
                return;
            }
            p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
        } catch (Exception e) {
            p.sendError(500, "Lỗi xử lý hóa đơn: " + e.getMessage());
        }
    }
}
```

**Giải thích luồng xử lý:**
1. Người dùng truy cập `/invoice?id=HD000001`
2. `doGet()` tải thông tin hóa đơn từ DB
3. Hiển thị `invoice.jsp` với chi tiết hóa đơn
4. Người dùng có thể:
   - Hủy hóa đơn (`op=cancel`)
   - Thanh toán cọc (`op=pay`)
   - Thanh toán phần còn lại (`op=payRemaining`)
   - Áp dụng voucher (`op=applyVoucher`)

### 5.2 InvoiceManagementServlet.java
**Đường dẫn:** `/admin/invoice-management`
**Chức năng:** Quản lý thanh toán và hủy hóa đơn từ phía quản trị

**Code hoàn chỉnh:**
```java
package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Servlet xử lý các chức năng thanh toán và hủy hóa đơn từ phía quản trị.
 * Hiện tại giao diện quản lý danh sách đã bị loại bỏ, chỉ giữ lại logic xử lý thanh toán.
 */
@WebServlet("/admin/invoice-management")
public class InvoiceManagementServlet extends HttpServlet {
    private final BookingService service = new BookingService();

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            String op = q.getParameter("op");
            String id = q.getParameter("id");

            // Hiển thị form thanh toán cho một hóa đơn cụ thể
            if ("paymentForm".equals(op)) {
                Map<String, Object> invoice = service.invoice(id);
                if (invoice == null) {
                    p.sendRedirect(q.getContextPath() + "/admin");
                    return;
                }
                q.setAttribute("invoice", invoice);
                q.getRequestDispatcher("/jsp/admin/invoice-payment.jsp").forward(q, p);
                return;
            }

            // Các yêu cầu khác đều quay về Dashboard
            p.sendRedirect(q.getContextPath() + "/admin");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            String op = q.getParameter("op");
            String id = q.getParameter("id");
            
            // Xử lý các hành động thanh toán hoặc hủy hóa đơn
            if ("cancel".equals(op)) {
                service.cancelInvoice(id);
            } else if ("pay".equals(op)) {
                service.payDeposit(id); // Thanh toán tiền cọc
            } else if ("payRemaining".equals(op)) {
                service.payRemaining(id); // Thanh toán phần còn lại
            }
            p.sendRedirect(q.getContextPath() + "/admin");
        } catch (Exception e) { 
            throw new ServletException(e); 
        }
    }
}
```

**Giải thích:**
- `doGet()`: Hiển thị form thanh toán (`invoice-payment.jsp`)
- `doPost()`: Xử lý thanh toán cọc, thanh toán phần còn lại, hoặc hủy

---

## 6. CÁC SERVICE

### 6.1 VoucherService.java
**Đường dẫn:** `src/main/java/com/footballbooking/service/VoucherService.java`
**Chức năng:** Tầng Service xử lý nghiệp vụ Voucher

**Code hoàn chỉnh:**
```java
package com.footballbooking.service;

import com.footballbooking.model.Entities.Voucher;
import com.footballbooking.model.Entities.VoucherLoai;
import com.footballbooking.repository.VoucherRepository;

import java.util.List;
import java.util.Map;

/**
 * Tầng Service cho nghiệp vụ Voucher — lọc theo loai, CRUD.
 */
public class VoucherService {
    private final VoucherRepository repo = new VoucherRepository();

    public List<Voucher> getAll() throws Exception {
        return repo.getAll();
    }

    public Voucher getById(String maVoucher) throws Exception {
        return repo.getById(maVoucher);
    }

    public Voucher getByCode(String maCode) throws Exception {
        return repo.getByCode(maCode);
    }

    public List<Voucher> getVoucherByLoai(int loai) throws Exception {
        return repo.getVoucherByLoai(loai);
    }

    public List<Voucher> getVoucherHoaDon() throws Exception {
        return repo.getVoucherHoaDon();
    }

    public List<Voucher> getVoucherSan() throws Exception {
        return repo.getVoucherSan();
    }

    public List<Voucher> getVoucherDichVu() throws Exception {
        return repo.getVoucherDichVu();
    }

    /** Voucher cho trang Booking (chỉ loai = 1). */
    public List<Map<String, Object>> getAvailableBookingVouchers(double donHang) throws Exception {
        return repo.toMapList(repo.getAvailableHoaDonVouchers(donHang));
    }

    public List<Map<String, Object>> getVoucherSanAsMaps() throws Exception {
        return repo.toMapList(repo.getVoucherSan());
    }

    public List<Map<String, Object>> getVoucherDichVuAsMaps() throws Exception {
        return repo.toMapList(repo.getVoucherDichVu());
    }

    public List<Map<String, Object>> getVoucherByLoaiAsMaps(int loai) throws Exception {
        return repo.toMapList(repo.getVoucherByLoai(loai));
    }

    public int insert(Voucher v) throws Exception {
        if (v.getLoai() <= 0) v.setLoai(VoucherLoai.HOA_DON);
        if (v.getTrangThai() == null || v.getTrangThai().isBlank()) v.setTrangThai("HOẠT ĐỘNG");
        return repo.insert(v);
    }

    public int update(Voucher v) throws Exception {
        return repo.update(v);
    }

    public int delete(String maVoucher) throws Exception {
        return repo.delete(maVoucher);
    }

    public Map<String, Object> toMap(Voucher v) {
        return repo.toMap(v);
    }
}
```

**Giải thích các method quan trọng:**

### 6.1.1 getAvailableHoaDonVouchers()
```java
public List<Map<String, Object>> getAvailableBookingVouchers(double donHang)
```
**Mục đích:** Lấy danh sách voucher hóa đơn còn hiệu lực
**Điều kiện:**
- `loai = 1` (Hóa đơn)
- `trang_thai = HOẠT ĐỘNG`
- `ngay_bat_dau <= hiện_tại <= ngay_ket_thuc`
- `so_luong > 0`
- `don_toi_thieu <= tổng tiền hóa đơn`

### 6.1.2 toMap()
```java
public Map<String, Object> toMap(Voucher v)
```
**Mục đích:** Chuyển đổi Entity Voucher sang Map để hiển thị trên JSP
**Lý do:** JSP EL không truy cập được private field, cần chuyển sang Map

---

## 7. CÁC REPOSITORY

### 7.1 VoucherRepository.java
**Đường dẫn:** `src/main/java/com/footballbooking/repository/VoucherRepository.java`
**Chức năng:** Repository quản lý Voucher

**Code hoàn chỉnh:**
```java
package com.footballbooking.repository;

import com.footballbooking.config.DBConnection;
import com.footballbooking.model.Entities.Voucher;
import com.footballbooking.model.Entities.VoucherLoai;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO quản lý bảng Voucher.
 * Hỗ trợ CRUD đầy đủ và lọc theo cột loai (1=Hóa đơn, 2=Sân, 3=Dịch vụ).
 */
public class VoucherRepository {

    private static final String COLS =
            "ma_voucher, ma_code, ten_voucher, loai_giam, gia_tri, don_toi_thieu, "
                    + "ngay_bat_dau, ngay_ket_thuc, so_luong, trang_thai, loai";

    static {
        ensureSchema();
    }

    /**
     * Đảm bảo bảng Voucher / cột loai / cột tien_giam tồn tại.
     */
    private static void ensureSchema() {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("IF OBJECT_ID('Voucher', 'U') IS NULL CREATE TABLE Voucher ("
                    + "ma_voucher VARCHAR(20) PRIMARY KEY, "
                    + "ma_code VARCHAR(50) NOT NULL, "
                    + "ten_voucher NVARCHAR(100) NOT NULL, "
                    + "loai_giam VARCHAR(20) NOT NULL, "
                    + "gia_tri FLOAT NOT NULL DEFAULT 0, "
                    + "don_toi_thieu FLOAT NOT NULL DEFAULT 0, "
                    + "ngay_bat_dau DATE NOT NULL, "
                    + "ngay_ket_thuc DATE NOT NULL, "
                    + "so_luong INT NOT NULL DEFAULT 0, "
                    + "trang_thai NVARCHAR(30) NOT NULL DEFAULT N'HOẠT ĐỘNG', "
                    + "loai INT NOT NULL DEFAULT 1)");

            // Bổ sung cột loai nếu bảng cũ chưa có
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
            s.execute("IF COL_LENGTH('Voucher','ma_voucher') IS NULL AND COL_LENGTH('Voucher','idVoucher') IS NOT NULL "
                    + "EXEC sp_rename 'Voucher.idVoucher', 'ma_voucher', 'COLUMN'");

            s.execute("IF COL_LENGTH('HoaDon','tien_giam') IS NULL ALTER TABLE HoaDon ADD tien_giam DECIMAL(18,2) NOT NULL DEFAULT 0");
            s.execute("IF COL_LENGTH('HoaDon','ma_voucher') IS NULL ALTER TABLE HoaDon ADD ma_voucher VARCHAR(20) NULL");
        } catch (Exception e) {
            System.err.println("VoucherRepository schema: " + e.getMessage());
        }
    }

    /** Lấy tất cả voucher (đầy đủ cột loai). */
    public List<Voucher> getAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM Voucher ORDER BY loai, ma_voucher";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            return mapList(rs);
        }
    }

    /** Lấy voucher theo mã. */
    public Voucher getById(String maVoucher) throws SQLException {
        if (maVoucher == null || maVoucher.isBlank()) return null;
        String sql = "SELECT " + COLS + " FROM Voucher WHERE ma_voucher=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, maVoucher.trim());
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Lấy voucher theo mã code (VD: MORNING10, PROMO50). */
    public Voucher getByCode(String maCode) throws SQLException {
        if (maCode == null || maCode.isBlank()) return null;
        String sql = "SELECT " + COLS + " FROM Voucher WHERE ma_code=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, maCode.trim());
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Lọc voucher theo loại áp dụng (1/2/3...). */
    public List<Voucher> getVoucherByLoai(int loai) throws SQLException {
        String sql = "SELECT " + COLS + " FROM Voucher WHERE loai=? ORDER BY ma_voucher";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, loai);
            try (ResultSet rs = p.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /** Voucher áp dụng cho Hóa đơn (Booking) — loai = 1. */
    public List<Voucher> getVoucherHoaDon() throws SQLException {
        return getVoucherByLoai(VoucherLoai.HOA_DON);
    }

    /** Voucher áp dụng cho Sân — loai = 2. */
    public List<Voucher> getVoucherSan() throws SQLException {
        return getVoucherByLoai(VoucherLoai.SAN);
    }

    /** Voucher áp dụng cho Dịch vụ — loai = 3. */
    public List<Voucher> getVoucherDichVu() throws SQLException {
        return getVoucherByLoai(VoucherLoai.DICH_VU);
    }

    /**
     * Voucher Hóa đơn còn hiệu lực (ngày, số lượng, trạng thái) cho trang Booking.
     */
    public List<Voucher> getAvailableHoaDonVouchers(double donHang) throws SQLException {
        String sql = "SELECT " + COLS + " FROM Voucher "
                + "WHERE loai=? "
                + "AND (trang_thai = N'HOẠT ĐỘNG') "
                + "AND (ngay_bat_dau <= CAST(SYSDATETIME() AS DATE)) "
                + "AND (ngay_ket_thuc >= CAST(SYSDATETIME() AS DATE)) "
                + "AND (so_luong > 0) "
                + "AND (don_toi_thieu <= ?) "
                + "ORDER BY ma_code";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, VoucherLoai.HOA_DON); // 1 = HOA_DON
            p.setDouble(2, donHang);
            try (ResultSet rs = p.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /** Thêm voucher mới. */
    public int insert(Voucher v) throws SQLException {
        String sql = "INSERT INTO Voucher (" + COLS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            bindAll(p, v);
            return p.executeUpdate();
        }
    }

    /** Cập nhật voucher. */
    public int update(Voucher v) throws SQLException {
        String sql = "UPDATE Voucher SET ma_code=?, ten_voucher=?, loai_giam=?, gia_tri=?, don_toi_thieu=?, "
                + "ngay_bat_dau=?, ngay_ket_thuc=?, so_luong=?, trang_thai=?, loai=? "
                + "WHERE ma_voucher=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, v.getMaCode());
            p.setString(2, v.getTenVoucher());
            p.setString(3, v.getLoaiGiam());
            p.setDouble(4, v.getGiaTri());
            p.setDouble(5, v.getDonToiThieu());
            setDate(p, 6, v.getNgayBatDau());
            setDate(p, 7, v.getNgayKetThuc());
            p.setInt(8, v.getSoLuong());
            p.setString(9, v.getTrangThai());
            p.setInt(10, v.getLoai());
            p.setString(11, v.getMaVoucher());
            return p.executeUpdate();
        }
    }

    /** Xóa voucher theo mã. */
    public int delete(String maVoucher) throws SQLException {
        if (maVoucher == null || maVoucher.isBlank()) return 0;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM Voucher WHERE ma_voucher=?")) {
            p.setString(1, maVoucher.trim());
            return p.executeUpdate();
        }
    }

    /** Chuyển entity sang Map (dùng cho JSP / Admin CRUD generic). */
    public Map<String, Object> toMap(Voucher v) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (v == null) return m;
        m.put("ma_voucher", v.getMaVoucher());
        m.put("ma_code", v.getMaCode());
        m.put("ten_voucher", v.getTenVoucher());
        m.put("loai_giam", v.getLoaiGiam());
        m.put("gia_tri", v.getGiaTri());
        m.put("don_toi_thieu", v.getDonToiThieu());
        m.put("ngay_bat_dau", v.getNgayBatDau() != null ? v.getNgayBatDau().toString() : null);
        m.put("ngay_ket_thuc", v.getNgayKetThuc() != null ? v.getNgayKetThuc().toString() : null);
        m.put("so_luong", v.getSoLuong());
        m.put("trang_thai", v.getTrangThai());
        m.put("loai", v.getLoai());
        m.put("ten_loai", v.getTenLoai());
        // Alias tương thích hiển thị cũ trên booking (nếu còn tham chiếu)
        m.put("idVoucher", v.getMaVoucher());
        m.put("mucGiam", v.getGiaTri());
        m.put("loaiGiam", v.getLoaiGiam());
        m.put("maCode", v.getMaCode());
        m.put("tenVoucher", v.getTenVoucher());
        return m;
    }

    public List<Map<String, Object>> toMapList(List<Voucher> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (list != null) {
            for (Voucher v : list) out.add(toMap(v));
        }
        return out;
    }

    private void bindAll(PreparedStatement p, Voucher v) throws SQLException {
        p.setString(1, v.getMaVoucher());
        p.setString(2, v.getMaCode());
        p.setString(3, v.getTenVoucher());
        p.setString(4, v.getLoaiGiam());
        p.setDouble(5, v.getGiaTri());
        p.setDouble(6, v.getDonToiThieu());
        setDate(p, 7, v.getNgayBatDau());
        setDate(p, 8, v.getNgayKetThuc());
        p.setInt(9, v.getSoLuong());
        p.setString(10, v.getTrangThai() != null ? v.getTrangThai() : "HOẠT ĐỘNG");
        p.setInt(11, v.getLoai() > 0 ? v.getLoai() : VoucherLoai.HOA_DON);
    }

    private static void setDate(PreparedStatement p, int idx, LocalDate d) throws SQLException {
        if (d == null) p.setNull(idx, java.sql.Types.DATE);
        else p.setDate(idx, Date.valueOf(d));
    }

    private List<Voucher> mapList(ResultSet rs) throws SQLException {
        List<Voucher> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Voucher mapRow(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setMaVoucher(getStr(rs, "ma_voucher"));
        v.setMaCode(getStr(rs, "ma_code"));
        v.setTenVoucher(getStr(rs, "ten_voucher"));
        v.setLoaiGiam(getStr(rs, "loai_giam"));
        v.setGiaTri(getDbl(rs, "gia_tri"));
        v.setDonToiThieu(getDbl(rs, "don_toi_thieu"));
        Date d1 = rs.getDate("ngay_bat_dau");
        Date d2 = rs.getDate("ngay_ket_thuc");
        v.setNgayBatDau(d1 != null ? d1.toLocalDate() : null);
        v.setNgayKetThuc(d2 != null ? d2.toLocalDate() : null);
        v.setSoLuong(getInt(rs, "so_luong"));
        v.setTrangThai(getStr(rs, "trang_thai"));
        v.setLoai(getInt(rs, "loai"));
        // Fallback schema cũ (nếu cột mới null nhưng còn cột legacy)
        if (v.getMaVoucher() == null) v.setMaVoucher(safeGet(rs, "idVoucher"));
        if (v.getLoaiGiam() == null) {
            String old = safeGet(rs, "loaiGiam");
            if ("GIAM_THEO_PHAN_TRAM".equals(old)) v.setLoaiGiam("PERCENT");
            else if (old != null) v.setLoaiGiam("MONEY");
        }
        if (v.getGiaTri() == 0) v.setGiaTri(safeDbl(rs, "mucGiam"));
        if (v.getDonToiThieu() == 0) v.setDonToiThieu(safeDbl(rs, "mucToiThieu"));
        if (v.getSoLuong() == 0) v.setSoLuong(safeInt(rs, "soLuong"));
        if (v.getNgayBatDau() == null) {
            Date d = safeDate(rs, "ngayBatDau");
            if (d != null) v.setNgayBatDau(d.toLocalDate());
        }
        if (v.getNgayKetThuc() == null) {
            Date d = safeDate(rs, "ngayKetThuc");
            if (d != null) v.setNgayKetThuc(d.toLocalDate());
        }
        if (v.getMaCode() == null || v.getMaCode().isBlank()) v.setMaCode(v.getMaVoucher());
        if (v.getTenVoucher() == null || v.getTenVoucher().isBlank()) v.setTenVoucher(v.getMaCode());
        if (v.getLoai() == 0) v.setLoai(VoucherLoai.HOA_DON);
        if (v.getTrangThai() == null) v.setTrangThai("HOẠT ĐỘNG");
        return v;
    }

    private static String getStr(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    private static double getDbl(ResultSet rs, String col) {
        try {
            double d = rs.getDouble(col);
            return rs.wasNull() ? 0 : d;
        } catch (SQLException e) { return 0; }
    }

    private static int getInt(ResultSet rs, String col) {
        try {
            int i = rs.getInt(col);
            return rs.wasNull() ? 0 : i;
        } catch (SQLException e) { return 0; }
    }

    private static String safeGet(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }

    private static double safeDbl(ResultSet rs, String col) {
        try { return rs.getDouble(col); } catch (Exception e) { return 0; }
    }

    private static int safeInt(ResultSet rs, String col) {
        try { return rs.getInt(col); } catch (Exception e) { return 0; }
    }

    private static Date safeDate(ResultSet rs, String col) {
        try { return rs.getDate(col); } catch (Exception e) { return null; }
    }
}
```

**Lưu ý:** VoucherRepository có 330 dòng, xử lý CRUD voucher và tích hợp với BookingRepository.

---

## 8. CÁC JSP

### 8.1 invoice.jsp (Xem hóa đơn)
**Đường dẫn:** `/jsp/user/invoice.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.invoice-box {
    max-width: 800px;
    margin: 30px auto;
    padding: 30px;
    border: 1px solid #ddd;
    border-radius: 8px;
    background: #fff;
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}
.invoice-header {
    text-align: center;
    border-bottom: 2px solid #1565c0;
    padding-bottom: 15px;
    margin-bottom: 20px;
}
.invoice-header h2 {
    color: #1565c0;
    margin: 0;
}
.invoice-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20px;
}
.invoice-info div {
    flex: 1;
}
.invoice-table {
    width: 100%;
    border-collapse: collapse;
    margin: 20px 0;
}
.invoice-table th,
.invoice-table td {
    border: 1px solid #ddd;
    padding: 10px;
    text-align: left;
}
.invoice-table th {
    background: #f5f5f5;
    font-weight: bold;
}
.invoice-total {
    text-align: right;
    margin-top: 20px;
    padding-top: 15px;
    border-top: 2px solid #333;
}
.invoice-total p {
    margin: 8px 0;
    font-size: 16px;
}
.invoice-total .final-total {
    font-size: 22px;
    font-weight: bold;
    color: #c62828;
}
.btn-group {
    margin-top: 25px;
    display: flex;
    gap: 10px;
    justify-content: center;
}
.btn-pay {
    background: #4caf50;
    color: white;
    padding: 12px 25px;
    border-radius: 5px;
    text-decoration: none;
    font-weight: bold;
    border: none;
    cursor: pointer;
    font-size: 15px;
}
.btn-cancel {
    background: #f44336;
    color: white;
    padding: 12px 25px;
    border-radius: 5px;
    text-decoration: none;
    font-weight: bold;
    border: none;
    cursor: pointer;
    font-size: 15px;
}
.status-badge {
    display: inline-block;
    padding: 5px 12px;
    border-radius: 15px;
    font-size: 13px;
    font-weight: bold;
}
.status-unpaid { background: #fff3e0; color: #e65100; }
.status-deposited { background: #e3f2fd; color: #1565c0; }
.status-paid { background: #e8f5e9; color: #2e7d32; }
.status-cancelled { background: #ffebee; color: #c62828; }
</style>
<div class="invoice-box">
    <div class="invoice-header">
        <h2>HÓA ĐƠN ĐẶT SÂN</h2>
        <p>Sân Bóng Hoa Sen</p>
    </div>

    <div class="invoice-info">
        <div>
            <p><strong>Mã hóa đơn:</strong> ${invoice.ma_hd}</p>
            <p><strong>Khách hàng:</strong> ${invoice.ho_ten}</p>
            <p><strong>Số điện thoại:</strong> ${invoice.sdt}</p>
        </div>
        <div>
            <p><strong>Ngày lập:</strong> ${invoice.ngay_lap}</p>
            <p><strong>Hạn thanh toán:</strong> ${invoice.han_thanh_toan}</p>
            <p><strong>Trạng thái:</strong> 
                <span class="status-badge ${invoice.trang_thai == 'ĐÃ THANH TOÁN' ? 'status-paid' : invoice.trang_thái == 'MỚI THANH TOÁN CỌC' ? 'status-deposited' : invoice.trang_thai == 'ĐÃ HỦY' ? 'status-cancelled' : 'status-unpaid'}">
                    ${invoice.trang_thai}
                </span>
            </p>
        </div>
    </div>

    <h3 style="color:#1565c0;margin-top:25px;">Chi tiết sân đã đặt</h3>
    <table class="invoice-table">
        <thead>
            <tr>
                <th>Tên sân</th>
                <th>Loại sân</th>
                <th>Ngày đặt</th>
                <th>Ca</th>
                <th>Đơn giá</th>
                <th>Thành tiền</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${invoice.slots}" var="s">
                <tr>
                    <td>${s.ten_san}</td>
                    <td>${s.loai_san}</td>
                    <td>${s.ngay_dat}</td>
                    <td>${s.buoi} ${s.ca}</td>
                    <td><fmt:formatNumber value="${s.thanh_tien}" type="number"/>đ</td>
                    <td style="font-weight:bold;color:#c62828;"><fmt:formatNumber value="${s.thanh_tien}" type="number"/>đ</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

    <h3 style="color:#1565c0;margin-top:25px;">Dịch vụ đi kèm</h3>
    <table class="invoice-table">
        <thead>
            <tr>
                <th>Tên dịch vụ</th>
                <th>Số lượng</th>
                <th>Đơn giá</th>
                <th>Thành tiền</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${invoice.services}" var="sv">
                <tr>
                    <td>${sv.ten_dv}</td>
                    <td>${sv.so_luong}</td>
                    <td><fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</td>
                    <td style="font-weight:bold;color:#c62828;"><fmt:formatNumber value="${sv.thanh_tien}" type="number"/>đ</td>
                </tr>
            </c:forEach>
            <c:if test="${empty invoice.services}">
                <tr><td colspan="4" style="text-align:center;color:#999;">Không có dịch vụ nào</td></tr>
            </c:if>
        </tbody>
    </table>

    <div class="invoice-total">
        <c:if test="${invoice.discount_amount > 0}">
            <p>Tạm tính: <fmt:formatNumber value="${invoice.raw_total}" type="number"/>đ</p>
            <p style="color:green;">Giảm giá: -<fmt:formatNumber value="${invoice.discount_amount}" type="number"/>đ</p>
        </c:if>
        <p>Tổng cộng: <b><fmt:formatNumber value="${invoice.tong_tien}" type="number"/>đ</b></p>
        <p>Tiền cọc (30%): <fmt:formatNumber value="${invoice.tien_coc}" type="number"/>đ</p>
        <p class="final-total">Còn lại: <fmt:formatNumber value="${invoice.so_tien_con_lai}" type="number"/>đ</p>
    </div>

    <div class="btn-group">
        <c:if test="${invoice.trang_thai == 'CHƯA THANH TOÁN' || invoice.trang_thai == 'ĐÃ ĐẶT'}">
            <form method="post" style="display:inline;">
                <input type="hidden" name="op" value="pay">
                <input type="hidden" name="id" value="${invoice.ma_hd}">
                <button type="submit" class="btn-pay" onclick="return confirm('Xác nhận thanh toán tiền cọc?')">
                    Thanh toán cọc (30%)
                </button>
            </form>
        </c:if>
        <c:if test="${invoice.trang_thai == 'MỚI THANH TOÁN CỌC'}">
            <form method="post" style="display:inline;">
                <input type="hidden" name="op" value="payRemaining">
                <input type="hidden" name="id" value="${invoice.ma_hd}">
                <button type="submit" class="btn-pay" onclick="return confirm('Xác nhận thanh toán phần còn lại?')">
                    Thanh toán phần còn lại (70%)
                </button>
            </form>
        </c:if>
        <c:if test="${invoice.trang_thai == 'CHƯA THANH TOÁN' || invoice.trang_thai == 'ĐANG XỬ LÝ'}">
            <form method="post" style="display:inline;" onsubmit="return confirm('Hủy hóa đơn?')">
                <input type="hidden" name="op" value="cancel">
                <input type="hidden" name="id" value="${invoice.ma_hd}">
                <button type="submit" class="btn-cancel">Hủy hóa đơn</button>
            </form>
        </c:if>
    </div>
</div>
<%@ include file="/jsp/common/footer.jspf" %>
```

**Giải thích:**
- Hiển thị chi tiết hóa đơn: thông tin KH, danh sách sân, dịch vụ
- Tính tổng tiền, tiền cọc, tiền còn lại
- Nút thanh toán cọc (30%) hoặc thanh toán phần còn lại (70%)
- Nút hủy hóa đơn

---

## 9. LUỒNG XỬ LÝ

### 9.1 Luồng áp dụng Voucher

```
Người dùng chọn voucher
        ↓
Gửi POST /booking?op=applyVoucher&voucherId=VC001
        ↓
BookingService.applyVoucher()
        ↓
BookingRepository.applyVoucher()
        ↓
Kiểm tra voucher còn hiệu lực không
        ↓
Cập nhật HoaDon.ma_voucher
        ↓
Gọi updateTotals() để tính lại tiền giảm
        ↓
Tính toán: rawTotal * (giaTri/100) nếu PERCENT
        ↓
Cập nhật: tong_tien, tien_coc, so_tien_con_lai
        ↓
Giảm so_luong trong bảng Voucher
```

### 9.2 Luồng thanh toán cọc (30%)

```
Người dùng nhấn "Thanh toán cọc"
        ↓
Gửi POST /invoice?op=pay
        ↓
BookingService.payDeposit()
        ↓
BookingRepository.pay()
        ↓
Kiểm tra: HoaDon trạng_thái = CHƯA THANH TOÁN và hạn > hiện_tại
        ↓
Cập nhật: CHƯA THANH TOÁN → MỚI THANH TOÁN CỌC
        ↓
Cập nhật LichSan: ĐANG GIỮ CHỖ → ĐÃ ĐẶT
        ↓
Giảm so_luong Voucher
        ↓
Chuyển về /invoice?id=xxx
```

### 9.3 Luồng thanh toán phần còn lại (70%)

```
Người dùng nhấn "Thanh toán phần còn lại"
        ↓
Gửi POST /invoice?op=payRemaining
        ↓
BookingService.payRemaining()
        ↓
BookingRepository.payRemaining()
        ↓
Kiểm tra trạng_thái: UNPAID, DEPOSITED, hoặc BOOKED
        ↓
Cập nhật: → ĐÃ THANH TOÁN
        ↓
Cập nhật so_tien_con_lai = 0
        ↓
Giảm Voucher (nếu từ UNPAID)
        ↓
Chuyển về /invoice?id=xxx
```

### 9.4 Luồng tính tiền

```
Tổng tiền sân (rawTotal)
        ↓
Áp dụng Voucher (nếu có)
        ↓
discount = rawTotal * (giaTri / 100) [PERCENT]
        ↓
discount = giaTri [MONEY]
        ↓
finalTotal = rawTotal - discount
        ↓
tienCoc = finalTotal * 0.3 (30%)
        ↓
soTienConLai = finalTotal - tienCoc
        ↓
Cập nhật vào HoaDon
```

---

## 10. CÁC LỖI THƯỜNG GẶP

### 10.1 Lỗi voucher không áp dụng được

**Triệu chứng:** Chọn voucher nhưng tiền không giảm

**Nguyên nhân:**
- Voucher đã hết hạn
- Voucher đã hết số lượng
- Đơn hàng chưa đạt mức tối thiểu
- Trạng thái voucher không phải HOẠT ĐỘNG

**Cách sửa:**
```sql
-- Kiểm tra voucher
SELECT * FROM Voucher WHERE ma_voucher='VC001';

-- Cập nhật trạng thái
UPDATE Voucher SET trang_thai=N'HOẠT ĐỘNG', so_luong=10 WHERE ma_voucher='VC001';
```

### 10.2 Lỗi thanh toán thất bại

**Triệu chứng:** Nhấn thanh toán nhưng không cập nhật trạng thái

**Nguyên nhân:**
- Hóa đơn đã quá hạn (han_thanh_toan < hiện_tại)
- Trạng thái không phải CHƯA THANH TOÁN
- Lỗi kết nối DB

**Cách sửa:**
```sql
-- Kiểm tra trạng thái hóa đơn
SELECT ma_hd, trang_thai, han_thanh_toan FROM HoaDon WHERE ma_hd='HD000001';

-- Cập nhật lại hạn thanh toán
UPDATE HoaDon SET han_thanh_toan=DATEADD(MINUTE,60,SYSDATETIME()) WHERE ma_hd='HD000001';
```

### 10.3 Lỗi tính tiền sai

**Triệu chứng:** Tổng tiền không đúng

**Nguyên nhân:**
- CTHD chưa được tính lại
- Voucher áp dụng sai
- Tiền cọc tính sai

**Cách sửa:**
```java
// Gọi updateTotals() để tính lại
bookingRepository.updateTotals(hdId);

// Kiểm tra chi tiết hóa đơn
SELECT * FROM CTHD WHERE ma_hd='HD000001';
```

---

## 11. LƯU Ý QUAN TRỌNG

### 11.1 CLASS DÙNG CHUNG

1. **BookingRepository.java** (1189 dòng)
   - Người 3 cần đọc phần `invoice()`, `pay()`, `payRemaining()`, `cancelInvoice()`, `applyVoucher()`, `updateTotals()`
   - Không sửa code liên quan đến đặt sân của Người 2

2. **BookingService.java** (139 dòng)
   - Lớp trung gian, Người 3 phụ trách toàn bộ
   - Chỉ cần gọi các method từ Repository

3. **Entities.java** (196 dòng)
   - Chỉ quan tâm Voucher, HoaDon, CTHD
   - Không sửa các Entity khác

### 11.2 QUY TẮC CODE

1. **Luôn kiểm tra trạng thái hóa đơn trước khi thanh toán:**
```java
if (!"CHƯA THANH TOÁN".equals(status)) {
    throw new IllegalStateException("Hóa đơn đã thanh toán hoặc đã hủy");
}
```

2. **Sử dụng transaction khi cập nhật nhiều bảng:**
```java
c.setAutoCommit(false);
try {
    // Cập nhật HoaDon
    // Cập nhật LichSan
    // Giảm Voucher
    c.commit();
} catch (Exception e) {
    c.rollback();
    throw e;
}
```

3. **Tính toán tiền giảm chính xác:**
```java
if ("PERCENT".equals(loaiGiam)) {
    discount = rawTotal * (giaTri / 100.0);
} else {
    discount = giaTri;
}
if (discount > rawTotal) discount = rawTotal;
```

---

## 12. TÀI LIỆU THAM KHẢO

- **Java Servlet:** https://docs.oracle.com/javaee/8/tutorial/servlets.htm
- **JDBC:** https://docs.oracle.com/javase/tutorial/jdbc/
- **SQL Server:** https://docs.microsoft.com/en-us/sql/

---

**NGƯỜI PHỤ TRÁCH:** Người 3  
**FILE ĐẦU RA:** 03_Voucher_HoaDon_ThanhToan.md  
**TRẠNG THÁI:** Hoàn thành