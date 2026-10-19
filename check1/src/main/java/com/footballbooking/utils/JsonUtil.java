package com.footballbooking.utils;

import java.util.List;
import java.util.Map;

/**
 * Tiện ích chuyển đổi đối tượng Java sang chuỗi định dạng JSON.
 * Hỗ trợ các kiểu dữ liệu cơ bản: String, Number, Boolean, Map, List.
 * Dùng cho các phản hồi AJAX mà không cần thư viện bên thứ ba như Jackson hay GSON.
 */
public final class JsonUtil {
    private JsonUtil() {}

    /**
     * Chuyển đổi đối tượng bất kỳ sang chuỗi JSON.
     * @param value Đối tượng cần chuyển đổi
     * @return String Chuỗi JSON kết quả
     */
    @SuppressWarnings("unchecked")
    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escape((String) value) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) return object((Map<?, ?>) value);
        if (value instanceof List) return array((List<?>) value);
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    /**
     * Chuyển đổi một Map sang JSON object {key: value, ...}.
     */
    private static String object(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(stringify(String.valueOf(e.getKey()))).append(':').append(stringify(e.getValue()));
        }
        return sb.append('}').toString();
    }

    /**
     * Chuyển đổi một List sang JSON array [value1, value2, ...].
     */
    private static String array(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(stringify(list.get(i)));
        }
        return sb.append(']').toString();
    }

    /**
     * Xử lý thoát các ký tự đặc biệt trong chuỗi để đảm bảo định dạng JSON hợp lệ.
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
