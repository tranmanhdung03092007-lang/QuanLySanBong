# Cách merge code của 4 người

## Cấu trúc team/
- `person1_home_booking.java` — Người 1: Trang chủ & Đặt lịch
- `person2_invoice.java` — Người 2: Hóa đơn & Thanh toán
- `person3_admin_crud.java` — Người 3: Admin CRUD
- `person4_admin_invoice_api.java` — Người 4: Admin Quản lý Hóa đơn & API

## Hướng dẫn merge

### Bước 1: Tạo project chính
Đảm bảo project chính đã có đầy đủ các file **Shared** (không sửa):
- `pom.xml`
- `src/main/java/com/footballbooking/config/DBConnection.java`
- `src/main/java/com/footballbooking/config/ExpiryListener.java`
- `src/main/java/com/footballbooking/utils/JsonUtil.java`
- `src/main/java/com/footballbooking/utils/Slots.java`
- `src/main/webapp/css/style.css`
- `src/main/webapp/jsp/common/header.jspf`
- `src/main/webapp/jsp/common/footer.jspf`
- `src/main/webapp/WEB-INF/web.xml`
- `database/*.sql`

### Bước 2: Mỗi người tự tách code từ file `.java` và copy vào project

**Person 1** tách từ `team/person1_home_booking.java`:
1. Copy phần `BookingService.java` → `src/main/java/com/footballbooking/service/BookingService.java`
2. Copy phần `ScheduleApiServlet.java` → `src/main/java/com/footballbooking/controller/ScheduleApiServlet.java`
3. Copy phần `BookingServlet.java` → `src/main/java/com/footballbooking/controller/BookingServlet.java`
4. Copy phần `HomeServlet.java` → `src/main/java/com/footballbooking/controller/HomeServlet.java`
5. Copy phần `user/booking.jsp` → `src/main/webapp/jsp/user/booking.jsp`
6. Copy phần `user/home.jsp` → `src/main/webapp/jsp/user/home.jsp`
7. Copy phần `booking.js` → `src/main/webapp/js/booking.js`

**Person 2** tách từ `team/person2_invoice.java`:
1. Copy phần `InvoiceServlet.java` → `src/main/java/com/footballbooking/controller/InvoiceServlet.java`
2. Copy phần `user/invoice.jsp` → `src/main/webapp/jsp/user/invoice.jsp`
3. Copy phần `app.js` → `src/main/webapp/js/app.js`

**Person 3** tách từ `team/person3_admin_crud.java`:
1. Copy phần `AdminLoginServlet.java` → `src/main/java/com/footballbooking/controller/AdminLoginServlet.java`
2. Copy phần `AdminLogoutServlet.java` → `src/main/java/com/footballbooking/controller/AdminLogoutServlet.java`
3. Copy phần `AdminServlet.java` → `src/main/java/com/footballbooking/controller/AdminServlet.java`
4. Copy phần `AdminCrudServlet.java` → `src/main/java/com/footballbooking/controller/AdminCrudServlet.java`
5. Copy phần `admin/login.jsp` → `src/main/webapp/jsp/admin/login.jsp`
6. Copy phần `admin/dashboard.jsp` → `src/main/webapp/jsp/admin/dashboard.jsp`
7. Copy phần `admin/list.jsp` → `src/main/webapp/jsp/admin/list.jsp`
8. Copy phần `admin/form.jsp` → `src/main/webapp/jsp/admin/form.jsp`

**Person 4** tách từ `team/person4_admin_invoice_api.java`:
1. Copy phần `AdminInvoiceApiServlet.java` → `src/main/java/com/footballbooking/controller/AdminInvoiceApiServlet.java`
2. Copy phần `InvoiceManagementServlet.java` → `src/main/java/com/footballbooking/controller/InvoiceManagementServlet.java`
3. Copy phần `admin/invoice-management.jsp` → `src/main/webapp/jsp/admin/invoice-management.jsp`

### Bước 3: Build & Deploy
```bash
# Trong IntelliJ IDEA
1. Right-click pom.xml → Maven → Reload Project
2. Build → Rebuild Project
3. Chạy Tomcat
```

## Lưu ý
- Mỗi người chỉ sửa file của mình, không động vào Shared files
- Nếu có conflict, người sau merge phải thông báo cho người trước
- File `TEAM_SPLIT.md` có danh sách chi tiết phân công