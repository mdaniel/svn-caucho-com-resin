<%@ page import="example.HelloService" %>

<%@ page import="com.caucho.hessian.client.HessianProxyFactory" %>

<%@ page import="javax.naming.Context" %>
<%@ page import="javax.naming.InitialContext" %>

<%@ page import="javax.jms.Connection" %>
<%@ page import="javax.jms.ConnectionFactory" %>
<%@ page import="javax.jms.Destination" %>
<%@ page import="javax.jms.MessageConsumer" %>
<%@ page import="javax.jms.Message" %>
<%@ page import="javax.jms.Session" %>
<%@ page import="javax.jms.TextMessage" %>
<%
// Check for results

Context context = (Context) new InitialContext().lookup("java:comp/env");

ConnectionFactory connectionFactory = 
  (ConnectionFactory) context.lookup("jms/ConnectionFactory");

Destination resultQueue = (Destination) context.lookup("jms/ResultQueue");

Connection connection = connectionFactory.createConnection();
Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

MessageConsumer consumer = jmsSession.createConsumer(resultQueue);

connection.start();

Message message = consumer.receive(1);

if (message == null) {
  out.println("No results available");
} else if (message instanceof TextMessage) {
  out.println(((TextMessage) message).getText());
}

// Make a request

HessianProxyFactory factory = new HessianProxyFactory();

String url = "jms:jms/ServiceQueue";

HelloService hello = (HelloService) factory.create(HelloService.class, url);

hello.hello();
%>

