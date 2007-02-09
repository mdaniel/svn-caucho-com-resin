<%@ page import="javax.naming.*" %>
<%@ page import="example.HelloService" %>
<%
Context context = (Context) new InitialContext().lookup("java:comp/env");

HelloService hessianHello 
  = (HelloService) context.lookup("hessian/HelloService");

HelloService restHello 
  = (HelloService) context.lookup("rest/HelloService");

HelloService soapHello 
  = (HelloService) context.lookup("soap/HelloService");

HelloService vmHello 
  = (HelloService) context.lookup("service/HelloService");
%>
<pre>
From Hessian: <%= hessianHello.hello() %>
From REST: <%= restHello.hello() %>
From SOAP: <%= soapHello.hello() %>
From VM: <%= vmHello.hello() %>
</pre>
