<%@ page import="com.caucho.naming.Jndi" %>
<%@ page import="example.MathService" %>
<%
MathService math = (MathService) Jndi.lookup("hessian/MathService");
%>
<pre>
3 + 2 = <%= math.add(3, 2) %>
3 - 2 = <%= math.sub(3, 2) %>
3 * 2 = <%= math.mul(3, 2) %>
3 / 2 = <%= math.div(3, 2) %>
</pre>
