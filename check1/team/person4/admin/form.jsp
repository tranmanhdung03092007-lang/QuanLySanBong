<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container" style="max-width:500px;">
<p><a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}">&larr; Danh sách</a></p>
<h2>${isNew?'Thêm':'Sửa'} ${L[meta.table]}</h2>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<div class="card">
<form method="post">
<input type="hidden" name="entity" value="${meta.table}">
<input type="hidden" name="op" value="${isNew?'create':'update'}">
<c:forEach items="${meta.cols}" var="col">
<label>${L[col]!=null?L[col]:col}</label>
<c:choose>
<c:when test="${!isNew&&col==meta.pk}"><input name="${col}" value="${record[col]}" readonly style="background:#eee"></c:when>
<c:when test="${col=='trang_thai'}">
<select name="${col}">
<option ${record[col]=='CÒN TRỐNG'?'selected':''}>CÒN TRỐNG</option>
<option ${record[col]=='ĐANG GIỮ CHỖ'?'selected':''}>ĐANG GIỮ CHỖ</option>
<option ${record[col]=='ĐÃ ĐẶT'?'selected':''}>ĐÃ ĐẶT</option>
<option ${record[col]=='CHƯA THANH TOÁN'?'selected':''}>CHƯA THANH TOÁN</option>
<option ${record[col]=='ĐÃ THANH TOÁN'?'selected':''}>ĐÃ THANH TOÁN</option>
<option ${record[col]=='ĐÃ HỦY'?'selected':''}>ĐÃ HỦY</option>
<option ${record[col]=='HOẠT ĐỘNG'?'selected':''}>HOẠT ĐỘNG</option>
<option ${record[col]=='BẢO TRÌ'?'selected':''}>BẢO TRÌ</option>
</select>
</c:when>
<c:when test="${col=='email'}"><input type="email" name="${col}" value="${record[col]}"></c:when>
<c:when test="${col=='ngay_dat'}"><input type="date" name="${col}" value="${record[col]}"></c:when>
<c:otherwise><input name="${col}" value="${record[col]}" ${isNew?'required':''}></c:otherwise>
</c:choose>
</c:forEach>
<p><button class="btn">Lưu</button>
<a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}">Hủy</a></p>
</form></div></div>
<%@ include file="/jsp/common/footer.jspf" %>