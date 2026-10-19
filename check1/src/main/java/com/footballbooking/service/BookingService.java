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
    public String ensureInvoice(String hdId, String name, String phone, String email) throws Exception {
        return dao.ensureInvoice(hdId, name, phone, email);
    }
    public void toggleSlot(String hdId, String fieldId, LocalDate date, int ca) throws Exception {
        dao.toggleSlot(hdId, fieldId, date, ca);
    }
    public void updateService(String hdId, String serviceId, int qty) throws Exception {
        dao.updateService(hdId, serviceId, qty);
    }
    public boolean confirmDatSan(String id) throws Exception {
        return dao.confirmDatSan(id);
    }
}
