<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<html>
<head>
<title>EJB Stateless Hello Example</title>
</head>

<body>
<h1>EJB Stateless Hello Example</h1>

There are two client servlets in this example.<p>

<ol>
<li><a href="hello">hello</a> uses hessian directly to obtain a handle to the server
<li><a href="hello-jndi">hello-jndi</a> uses jndi to obtain a handle to the server
</ol>

</body>
</html>

