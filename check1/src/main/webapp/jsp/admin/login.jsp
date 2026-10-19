<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container" style="max-width:400px;">
<h2>Đăng nhập Quản trị</h2>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<div class="card">
<form method="post">
<label>Số điện thoại</label>
<input type="tel" name="sdt" value="${empty sdt ? '0365365113' : sdt}">
<label>Mật khẩu</label>
<input type="password" name="password" value="1">
<p><button class="btn">Đăng nhập</button>
<button type="button" class="btn" id="manualLogin">Tự động</button></p>
</form>
<p><small>Tài khoản: 0365365113 / 1</small></p>
<p><a href="${pageContext.request.contextPath}/home">&larr; Về trang chủ</a></p>
</div>
</div>
<script>document.getElementById('manualLogin')?.addEventListener('click',function(){document.querySelector('form').submit();});</script>
<%@ include file="/jsp/common/footer.jspf" %>