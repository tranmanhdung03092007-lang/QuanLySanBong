@echo off
REM Chạy script khởi tạo database
REM Yêu cầu: SQL Server đã cài, sqlcmd có trong PATH
sqlcmd -S localhost -U sa -P "123456@a" -i "database\football_booking.sql" -o "database_init.log"
if %ERRORLEVEL% EQU 0 (
    echo Database initialized successfully!
) else (
    echo Database initialization may have issues. Check database_init.log
)