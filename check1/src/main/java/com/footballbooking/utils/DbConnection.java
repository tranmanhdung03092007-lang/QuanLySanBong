package com.footballbooking.utils;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Lớp tiện ích quản lý kết nối cơ sở dữ liệu (Phiên bản cũ).
 * Lưu ý: Ưu tiên sử dụng com.footballbooking.config.DBConnection để có cấu hình linh hoạt hơn.
 */
public class DbConnection {

    // Đường dẫn kết nối CSDL SQL Server
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;databaseName=QuanLySanBong;encrypt=true;trustServerCertificate=true";

    // Thông tin đăng nhập mặc định
    private static final String USER = "sa";
    private static final String PASSWORD = "123456";

    /**
     * Tạo kết nối mới đến SQL Server.
     * @return Connection đối tượng kết nối hoặc null nếu lỗi
     */
    public static Connection getConnection() {
        try {
            // Tải driver SQL Server
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            // Thực hiện kết nối
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lớp nội bộ để kiểm tra kết nối nhanh.
     */
    public class TestConnection {
        public static void main(String[] args) {
            if (DbConnection.getConnection() != null) {
                System.out.println("Kết nối thành công");
            } else {
                System.out.println("Kết nối thất bại");
            }
        }
    }
}
