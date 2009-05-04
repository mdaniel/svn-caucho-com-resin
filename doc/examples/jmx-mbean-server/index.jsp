<%@ page import='javax.inject.Current, javax.management.*, example.TestAdmin' %>
<%!
@Current MBeanServer _server;
%><%
ObjectName name = new ObjectName("example:name=test");

out.println("data: " + _server.getAttribute(name, "Data"));
%>
