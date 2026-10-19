#!/usr/bin/env python3
import os

base = "team"
for p in range(1, 5):
    os.makedirs(f"{base}/person{p}", exist_ok=True)

# Person 1 files
p1 = """// BookingService.java
package com.footballbooking.service;
import com.footballbooking.repository.BookingRepository;
import com.footballbooking.utils.Slots;
import java.time.LocalDate;
import java.util.*;
public class BookingService {
    private final BookingRepository dao = new BookingRepository();
    public List<Map<String, Object>> fields() throws Exception { return dao.fields(); }
    public Map<String, Object> fullSchedule(LocalDate date) throws Exception {
        try {
            List<Map<String, Object>> fields = dao.fields();
            Map<String, List<Map<String, Object>>> grid = new LinkedHashMap<>();
            for (Map<String, Object> f : fields)
                grid.put(String.valueOf(f.get("ma_san")), dao.slots(String.valueOf(f.get("ma_san")), date));
            return pack(date, fields, grid);
        } catch (Exception e) { return packFallback(date); }
    }
    public Map<String, Object> emptySchedule(LocalDate date) { return packFallback(date); }
    private Map<String, Object> packFallback(LocalDate date) {
        List<Map<String, Object>> f = fallbackFields();
        Map<String, List<Map<String, Object>>> grid = new LinkedHashMap<>();
        for (Map<String, Object> x : f) grid.put(String.valueOf(x.get("ma_san")), freeSlots());
        return pack(date, f, grid);
    }
    private Map<String, Object> pack(LocalDate d, List<Map<String, Object>> f, Map<String, List<Map<String, Object>>> g) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("date", d.toString()); r.put("fields", f); r.put("schedule", g); r.put("headers", makeHeaders());
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
    private List<Map<String, Object>> fallbackFields() {
        List<Map<String, Object>> l = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ma_san", "S0" + i); r.put("ten_san", "Sân " + i);
            r.put("loai_san", "Sân 7 người"); r.put("gia_theo_gio", 400000 + i * 50000);
            r.put("trang_thai", "HOẠT ĐỘNG"); l.add(r);
        }
        return l;
    }
    public String book(String fieldId, LocalDate date, int ca, String name, String phone) throws Exception {
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Không thể đặt ngày trong quá khứ");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Họ tên trống");
        if (phone == null || !phone.matches("^(0|\\+84)[0-9]{9}$")) throw new IllegalArgumentException("Số điện thoại sai");
        if (!Slots.valid(ca)) throw new IllegalArgumentException("Ca không hợp lệ");
        return dao.createBooking(fieldId, date, ca, name.trim(), phone.trim());
    }
    public Map<String, Object> invoice(String id) throws Exception { return dao.invoice(id); }
    public boolean pay(String id) throws Exception { return dao.pay(id); }
}
"""

with open(f"{base}/person1/BookingService.java", "w", encoding="utf-8") as f:
    f.write(p1.strip() + "\n")
print("Created person1/BookingService.java")

</script>