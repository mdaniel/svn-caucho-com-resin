<pre>
&lt;%@ taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>

&lt;%@ include file="/inc/nobrowsercache.jspf" %>

&lt;%-- /index.jsp - default page for website. --%>


&lt;html>
  &lt;head>
    &lt;title>Hogwart's&lt;/title>
  &lt;/head>

  &lt;body>
    &lt;%@ include file="/inc/buttonbar.jspf" %>

    &lt;h1>Welcome to Hogwart's!&lt;/h1>

    This is a Defense Against the Dark Arts example of using
    JSP/Servlet security. 
    &lt;a href="&lt;c:url value='index.xtp'/>">Tutorial documentation&lt;/a> is 
    available.
    &lt;p>

    Try doing a 
    &lt;c:choose>
      &lt;c:when test="${empty pageContext.request.userPrincipal}">
        &lt;a href="&lt;c:url value='home.jsp'/>">login&lt;/a>
      &lt;/c:when>
      &lt;c:otherwise>
        &lt;a href="&lt;c:url value='logout.jsp'/>">logout&lt;/a>
      &lt;/c:otherwise>
    &lt;/c:choose>

    &lt;p>
    To get a better understanding of how security works, try using
    the following links both when you are logged in and when you are
    not.
    &lt;p>
    All of the links are in secure areas.  If you are not
    logged in a login procedure is put in by Resin before you get
    to the pages.  If you are logged in, you may be able to see them 
    or you may get a 'Forbidden' error.
    &lt;p>
    Links to different areas:
    &lt;ul>
      &lt;li>&lt;a href="&lt;c:url value='students/'/>">
	    Students (available to 'students' and 'professors')
	  &lt;/a>
      &lt;li>&lt;a href="&lt;c:url value='professors/'/>">
	    Professors (available to 'professors')
	  &lt;/a>
      &lt;li>&lt;a href="&lt;c:url value='staff/'/>">
	    Staff (available to 'staff' and 'professors')
	  &lt;/a>
    &lt;/ul>

    In a real application, you wouldn't show links like this -- you
    would get the user to login first and then only display the links
    that are available for their role.

    &lt;%@ include file="/inc/footer.jspf" %>
  &lt;/body>
&lt;/html>
