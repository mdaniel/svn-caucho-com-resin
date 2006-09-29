<%@ page import="com.caucho.naming.Jndi" %>
<%@ page import="example.HelloService" %>
<%
HelloService hessianHello = (HelloService) Jndi.lookup("hessian/HelloService");
HelloService restHello = (HelloService) Jndi.lookup("rest/HelloService");
HelloService soapHello = (HelloService) Jndi.lookup("soap/HelloService");
HelloService vmHello = (HelloService) Jndi.lookup("service/HelloService");
%>
<pre>
From Hessian: <%= hessianHello.hello() %>
From REST: <%= restHello.hello() %>
From SOAP: <%= soapHello.hello() %>
From VM: <%= vmHello.hello() %>
</pre>
