<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator"
           prefix="decorator" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
	    <title>Cineasts.net - <decorator:title default="Welcome" /></title>
	    <decorator:head />
	    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/style.css"/>" />
	    <%@ include file="/WEB-INF/views/includes/style.jsp" %>
	    <%@ include file="/WEB-INF/views/includes/js.jsp" %>
	</head>
	<body>
		<div id="header">
			<div id="header-topbar">
			    <div id="header-menu">
					<page:applyDecorator name="header_search">
						<%@ include file="/WEB-INF/views/includes/search.jsp" %>
					</page:applyDecorator>
					<%@ include file="/WEB-INF/views/includes/navigation.jsp" %>
				</div>
			</div>
			<a href="<c:url value="/"/>" id="logo"><img src="<c:url value="/images/logo.png"/>" /></a>
		</div>
	    <div id="content">
            <decorator:body />
	    </div>
	    <%@ include file="/WEB-INF/views/includes/footer.jsp" %>
	</body>
</html>