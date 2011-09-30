<pre>
&lt;%@ page import="java.util.List" %>
&lt;%@ page import="javax.naming.*" %>
&lt;%@ page import="javax.xml.ws.Holder" %>
&lt;%@ page import="example.UserService" %>
&lt;%@ page import="example.User" %>
&lt;%
Context context = (Context) new InitialContext().lookup("java:comp/env");

UserService service = (UserService) context.lookup("soap/UserService");
List<User> users = service.getUsers(2);

Exception invalid = null;

try {
  service.getUsers(0);
}
catch (Exception e) {
  invalid = e;
}
%>
&lt;pre>
UserService.getUsers(1): &lt;%= users %>
UserService.getUsers(0): &lt;%= invalid %>
&lt;/pre>
