<%@ page import='javax.naming.*, javax.management.*, example.TestAdmin' %>
<%
Context ic = new InitialContext();

MBeanServer server = (MBeanServer) ic.lookup("java:comp/env/jmx/MBeanServer");

ObjectName name = new ObjectName("example:name=test");

out.println("data: " + server.getAttribute(name, "Data"));
%>
