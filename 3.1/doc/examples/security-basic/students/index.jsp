<%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>

<%@ include file="/inc/nobrowsercache.jspf" %>

<html>
  <head>
    <title>Hogwart's::Student's</title>
  </head>

  <body>
    <%@ include file="/inc/buttonbar.jspf" %>
    <h1>Student's Home</h1>

    Welcome <c:out value="${pageContext.request.remoteUser}"/>.

    <%@ include file="/inc/footer.jspf" %>
  </body>
</html>
