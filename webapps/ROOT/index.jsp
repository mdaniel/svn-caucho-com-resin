<%@ page session="false" import="com.caucho.vfs.*,com.caucho.http.webapp.*" %>

<%-- 
  This is the default start page for the Resin server.
  --%>

<%
/**
 * See if the resin-doc webapp is installed
 */
boolean hasResinDoc = false;
boolean hasOrientation = false;

%>

<html>
<head><title>Resin&#174; Default Home Page</title></head>

<body>
<h1 style="background: #ccddff">Resin&#174; Default Home Page</h1>

This is the default page for the Resin web server.

<% if (hasResinDoc) { %>

<p>Documentation is available at <a href="http://www.caucho.com">www.caucho.com</a>.

<p>Administration is available at <a href="/resin-admin">/resin-admin</a>.

<% } else { %>
<p>
Documentation is available at <a href="http://www.caucho.com">www.caucho.com</a>.
<% }  %>
</body>

</html>
