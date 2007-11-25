<%@ page import='javax.webbeans.In, javax.management.*, example.TestAdmin' %>
<%!
@In MBeanServer _server;
%><%
ObjectName name = new ObjectName("example:name=test");

out.println("data: " + _server.getAttribute(name, "Data"));
%>
