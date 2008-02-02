<%@ page import="javax.webbeans.Named" %>
<%@ page import="example.HelloService" %>
<%!
@Named("hessian") HelloService _hessianHello; 
@Named("burlap") HelloService _burlapHello; 
@Named("vm") HelloService _vmHello; 
%>
<pre>
From Hessian: <%= _hessianHello.hello() %>
From Burlap: <%= _burlapHello.hello() %>
From VM: <%= _vmHello.hello() %>
</pre>

<ul>
<li><a href="demo.php">PHP</a></li>
<li><a href="index.xtp">Tutorial</a></li>
<ul>
