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
     * @param ca Số thứ tự ca (1-9)
     * @return Mảng chứa thông tin ca
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