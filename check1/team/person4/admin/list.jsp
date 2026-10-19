<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container">
<p><a class="btn" href="${pageContext.request.contextPath}/admin">&larr; Dashboard</a>
<a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}&action=form">+ Thêm</a></p>
<h2>Quản lý ${L[meta.table]}</h2>
<p>
<c:forEach items="${['San','LichSan','KhachHang','NhanVien','DichVu','HoaDon','CTHD']}" var="e">
<a class="btn" href="?entity=${e}">${L[e]}</a>
</c:forEach></p>
<form style="margin:10px 0;"><input type="hidden" name="entity" value="${meta.table}">
<input type="text" name="search" value="${param.search}" placeholder="Tìm..." style="width:200px;display:inline;">
<button class="btn">Tìm</button></form>
<div style="overflow-x:auto;">
<table><thead><tr>
<c:forEach items="${meta.cols}" var="col"><th>${L[col]!=null?L[col]:col}</th></c:forEach>
<th>Thao tác</th></tr></thead>
<tbody>
<c:forEach items="${rows}" var="row">
<tr><c:forEach items="${meta.cols}" var="col"><td>${row[col]}</td></c:forEach>
<td><a class="btn" href="?entity=${meta.table}&action=form&id=${row[meta.pk]}">Sửa</a>
<form method="post" style="display:inline;" onsubmit="return confirm('Xóa?')">
<input type="hidden" name="op" value="delete"><input type="hidden" name="${meta.pk}" value="${row[meta.pk]}">
<button class="btn">Xóa</button></form></td></tr>
</c:forEach>
<c:if test="${empty rows}"><tr><td colspan="20" style="text-align:center;padding:20px;">Chưa có dữ liệu</td></tr></c:if>
</tbody></table></div>
<p><c:forEach begin="1" end="${totalPages}" var="n">
<a class="btn" href="?entity=${meta.table}&page=${n}&search=${param.search}">${n}</a>
</c:forEach></p>
</div>
<%@ include file="/jsp/common/footer.jspf" %>