<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>

<div class="navigation">
    <c:choose>
        <c:when test="${empty user}">
            <a href="<c:url value="/auth/login" />">Login</a>
            <a href="<c:url value="/auth/registerpage" />">Register</a>
        </c:when>
        <c:otherwise>
            <a href="<c:url value="/user/${user.login}" />">${user.name}</a>
            <a href="<c:url value="/auth/logout" />">Logout</a>
        </c:otherwise>
    </c:choose>
</div>
