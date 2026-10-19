<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.dashboard-stats{display:flex;flex-wrap:wrap;gap:15px;margin:20px 0}
.stat-card{flex:1;min-width:150px;background:#fff;border:1px solid #e0e0e0;border-radius:8px;padding:20px;text-align:center;box-shadow:0 2px 4px rgba(0,0,0,0.05);transition:all 0.2s}
.stat-card:hover{transform:translateY(-5px);box-shadow:0 4px 12px rgba(42,82,152,0.2)}
.stat-value{font-size:28px;font-weight:bold;color:#1a3d7a;margin:10px 0}
.stat-label{color:#666;font-size:14px}
.management-grid{display:flex;flex-wrap:wrap;gap:10px;margin:20px 0}
.management-card{flex:1;min-width:120px;background:#2a5298;color:#fff;padding:15px;border-radius:8px;text-align:center;text-decoration:none;transition:all 0.2s}
.management-card:hover{background:#1a3d7a;transform:translateY(-3px)}
</style>
<div class="container">
<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px">
<div><h2 style="margin:0;color:#1a3d7a">Bảng điều khiển</h2>
<p style="color:#666">Xin chào, ${sessionScope.adminUser}</p></div>
<a class="btn btn-danger" href="${pageContext.request.contextPath}/admin/logout">Đăng xuất</a>
</div>

<!-- Statistics Cards -->
<div class="dashboard-stats">
<c:forEach items="${stats}" var="x">
<div class="stat-card">
<div class="stat-value">${x.value}</div>
<div class="stat-label">${x.key}</div>
</div>
</c:forEach>
</div>

<h3 style="color:#1a3d7a;margin-top:25px">Quản lý</h3>
<div class="management-grid">
<c:forEach items="${['San','LichSan','KhachHang','NhanVien','DichVu','HoaDon','CTHD']}" var="e">
<a class="management-card" href="${pageContext.request.contextPath}/admin/manage?entity=${e}">${L[e]}</a>
</c:forEach>
<a class="management-card" style="background:#4caf50" href="${pageContext.request.contextPath}/admin/field-dashboard">Quản lý Sân</a>
</div>

</div>
<%@ include file="/jsp/common/footer.jspf" %>