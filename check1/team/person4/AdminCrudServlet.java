package com.footballbooking.controller;
import com.footballbooking.config.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
@WebServlet("/admin/manage")
public class AdminCrudServlet extends HttpServlet {
    private final Map<String, String> L = Map.of(
        "San","Sân","LichSan","Lịch sân","KhachHang","Khách hàng","NhanVien","Nhân viên",
        "DichVu","Dịch vụ","HoaDon","Hóa đơn","CTHD","Chi tiết HĐ"
    );
    private final Map<String, String> PK = Map.of("San","ma_san","LichSan","ma_lich","KhachHang","ma_kh","NhanVien","ma_nv","DichVu","ma_dv","HoaDon","ma_hd","CTHD","ma_cthd");
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        HttpSession s = q.getSession(false);
        if (s == null || s.getAttribute("adminId") == null) { p.sendRedirect(q.getContextPath() + "/admin/login"); return; }
        String entity = q.getParameter("entity");
        if (entity == null || !L.containsKey(entity)) entity = "San";
        q.setAttribute("L", L);
        q.setAttribute("meta", meta(entity));
        if ("form".equals(q.getParameter("action"))) {
            q.setAttribute("isNew", q.getParameter("id") == null);
            q.setAttribute("record", "update".equals(q.getParameter("op")) ? read(entity, q.getParameter(PK.get(entity))) : new LinkedHashMap<>());
            q.getRequestDispatcher("/jsp/admin/form.jsp").forward(q, p);
        } else {
            int page = Math.max(1, parseInt(q.getParameter("page"), 1));
            int size = 10;
            String search = q.getParameter("search");
            List<Map<String, Object>> rows = list(entity, search, (page-1)*size, size);
            q.setAttribute("rows", rows);
            q.setAttribute("page", page);
            q.setAttribute("totalPages", Math.max(1, (count(entity, search) + size - 1) / size));
            q.getRequestDispatcher("/jsp/admin/list.jsp").forward(q, p);
        }
    }
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws IOException {
        HttpSession s = q.getSession(false);
        if (s == null || s.getAttribute("adminId") == null) { p.sendRedirect(q.getContextPath() + "/admin/login"); return; }
        String entity = q.getParameter("entity");
        String op = q.getParameter("op");
        String pk = PK.get(entity);
        if ("delete".equals(op)) {
            execute("DELETE FROM " + entity + " WHERE " + pk + "=?", q.getParameter(pk));
        } else if ("create".equals(op) || "update".equals(op)) {
            StringBuilder sql = new StringBuilder();
            List<String> cols = new ArrayList<>();
            List<String> vals = new ArrayList<>();
            java.util.Enumeration<String> names = q.getParameterNames();
            while (names.hasMoreElements()) {
                String n = names.nextElement();
                if (!n.equals("entity") && !n.equals("op") && !n.equals(pk)) {
                    cols.add(n); vals.add(q.getParameter(n));
                }
            }
            if ("update".equals(op)) {
                sql.append("UPDATE " + entity + " SET ");
                for (int i = 0; i < cols.size(); i++) { if (i > 0) sql.append(","); sql.append(cols.get(i)).append("=?"); }
                sql.append(" WHERE ").append(pk).append("=?");
                List<String> args = new ArrayList<>(vals); args.add(q.getParameter(pk));
                execute(sql.toString(), args.toArray());
            } else {
                sql.append("INSERT INTO ").append(entity).append("(").append(String.join(",", cols)).append(") VALUES(");
                for (int i = 0; i < cols.size(); i++) { if (i > 0) sql.append(","); sql.append("?"); }
                sql.append(")");
                execute(sql.toString(), vals.toArray());
            }
        }
        p.sendRedirect(q.getContextPath() + "/admin/manage?entity=" + entity);
    }
    private Map<String, Object> meta(String entity) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("table", entity);
        m.put("pk", PK.get(entity));
        m.put("cols", switch (entity) {
            case "San" -> List.of("ma_san","ten_san","loai_san","gia_theo_gio","trang_thai");
            case "LichSan" -> List.of("ma_lich","ma_san","ngay_dat","so_ca","ca","trang_thai");
            case "KhachHang" -> List.of("ma_kh","ho_ten","sdt","email");
            case "NhanVien" -> List.of("ma_nv","ten_nv","sdt","chuc_vu");
            case "DichVu" -> List.of("ma_dv","ten_dv","don_gia");
            case "HoaDon" -> List.of("ma_hd","ma_kh","tong_tien","tien_coc","trang_thai","han_thanh_toan");
            case "CTHD" -> List.of("ma_hd","ma_lich","so_luong","don_gia","thanh_tien");
            default -> List.of();
        });
        return m;
    }
    private List<Map<String, Object>> list(String entity, String search, int offset, int limit) {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            StringBuilder sql = new StringBuilder("SELECT * FROM " + entity + " WHERE 1=1");
            List<String> args = new ArrayList<>();
            if (search != null && !search.isEmpty()) {
                sql.append(" AND (");
                for (Object c : meta(entity).get("cols") instanceof List ? (List)meta(entity).get("cols") : List.of()) {
                    sql.append(c).append(" LIKE ? OR "); args.add("%"+search+"%");
                }
                sql.setLength(sql.length()-4); sql.append(")");
            }
            sql.append(" ORDER BY ").append(PK.get(entity)).append(" DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
            args.add(String.valueOf(offset)); args.add(String.valueOf(limit));
            PreparedStatement ps = c.prepareStatement(sql.toString());
            for (int i = 0; i < args.size(); i++) ps.setString(i+1, args.get(i));
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) { Map<String,Object> r = new LinkedHashMap<>(); for (int i = 1; i <= md.getColumnCount(); i++) r.put(md.getColumnName(i), rs.getObject(i)); l.add(r); }
        } catch (Exception e) {}
        return l;
    }
    private int count(String entity) throws Exception { return countWhere(entity, "1", "1"); }
    private int count(String entity, String search) throws Exception { return count(entity); }
    private int countWhere(String entity, String col, String val) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + entity + " WHERE " + col + "=?")) {
            ps.setString(1, val); ResultSet rs = ps.executeQuery(); return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }
    private Map<String, Object> read(String entity, String id) {
        Map<String, Object> r = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM " + entity + " WHERE " + PK.get(entity) + "=?")) {
            ps.setString(1, id); ResultSet rs = ps.executeQuery();
            if (rs.next()) { ResultSetMetaData md = rs.getMetaData(); for (int i = 1; i <= md.getColumnCount(); i++) r.put(md.getColumnName(i), rs.getObject(i)); }
        } catch (Exception e) {}
        return r;
    }
    private void execute(String sql, Object... args) {
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i+1, String.valueOf(args[i]));
            ps.executeUpdate();
        } catch (Exception e) {}
    }
    private int parseInt(String s, int d) { try { return Integer.parseInt(s); } catch (Exception e) { return d; } }
}