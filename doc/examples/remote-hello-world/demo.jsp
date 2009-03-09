<%@ page import="com.caucho.config.Name, javax.inject.Current" %>
<%@ page import="example.HelloService" %>
<%!
@Name("hessian") HelloService _hessianHello;
@Name("burlap") HelloService _burlapHello; 
@Current HelloService _vmHello; 
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
