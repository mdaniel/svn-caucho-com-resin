<%@ page session="false" import="com.caucho.vfs.*, com.caucho.server.webapp.*" %>

<%-- 
  This is the default start page for the Resin server.

  You can replace it as you wish, the documentation will
  still be available as /resin-doc if it is installed.
  --%>

<%
/**
 * See if the resin-doc webapp is installed
 */
boolean hasResinDoc = false;
boolean hasOrientation = false;

ServletContext docApp = application.getContext("/resin-doc");  

if (docApp != null) {
  String rp = docApp.getRealPath("index.xtp");

  if (rp != null && (new java.io.File(rp)).exists())
    hasResinDoc = true;

  if (hasResinDoc) {
    rp = docApp.getRealPath("orientation.xtp");
    if (rp != null && (new java.io.File(rp)).exists())
      hasOrientation = true;
  }
}
%>

<html>
<head><title>Resin&#174; Default Home Page</title></head>

<body>
<h1 style="background: #ccddff">Resin&#174; Default Home Page</h1>

This is the default page for the Resin web server.

<% if (hasResinDoc) { %>

<p>Documentation is available at <a href="/resin-doc">/resin-doc</a>.

<p>Administration is available at <a href="/resin-admin">/resin-admin</a>.

<% } else { %>
<p>
The Resin documentation is normally found with the url  <i>
<%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %>/resin-doc</i>, but it does not appear to be installed at that location.
<% }  %>
</body>

</html>
