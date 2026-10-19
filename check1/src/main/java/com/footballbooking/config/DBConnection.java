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
