<%@ page import="javax.webbeans.Named" %>
<%@ page import="example.HelloService" %>
<%!
@Named("hessian") HelloService _hessianHello; 
@Named("rest") HelloService _restHello; 
@Named("soap") HelloService _soapHello; 
@Named("vm") HelloService _vmHello; 
%>
<pre>
From Hessian: <%= _hessianHello.hello() %>
From REST: <%= _restHello.hello() %>
From SOAP: <%= _soapHello.hello() %>
From VM: <%= _vmHello.hello() %>
</pre>

<ul>
<li><a href="demo.php">PHP</a></li>
<li><a href="index.xtp">Tutorial</a></li>
<ul>
