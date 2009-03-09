<%@ page import="com.caucho.config.Name" %>
<%@ page import="example.MathService" %>
<%!
@Name("math") MathService math;
%>
<pre>
3 + 2 = <%= math.add(3, 2) %>
3 - 2 = <%= math.sub(3, 2) %>
3 * 2 = <%= math.mul(3, 2) %>
3 / 2 = <%= math.div(3, 2) %>
</pre>
<a href="demo.php">PHP demo</a>
