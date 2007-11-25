<%@ page import="javax.webbeans.In" %>
<%@ page import="example.MathService" %>
<%!
@In MathService math;
%>
<pre>
3 + 2 = <%= math.add(3, 2) %>
3 - 2 = <%= math.sub(3, 2) %>
3 * 2 = <%= math.mul(3, 2) %>
3 / 2 = <%= math.div(3, 2) %>
</pre>
