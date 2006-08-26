<%@ page contentType="text/html; charset=utf-8" %><html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
  <link rel="STYLESHEET" type="text/css" href="/resin-doc/css/default.css">
  <title>Local Stateless Session Hello</title>
  <meta name="description" content="Stateless sessions make database queries and updates robust by setting transaction boundaries at each business method. This stateless session bean example annotates a single business method with a SUPPORTS transaction attribute, marking the method as a read-only transaction boundary.">
  <meta name="keywords" content="j2ee caucho resin-ee index">
</head>
<body bgcolor="white" leftmargin="0">
<table cellpadding="0" cellspacing="0" border="0" width="100%" summary="">
<tr><td width="2"><img alt="" width="2" height="1" src="/resin-doc/images/pixel.gif"></td><td width="150"><img width="150" height="63" src="/resin-doc/images/caucho-white.jpg"></td><td width="10"><img alt="" width="10" height="1" src="/resin-doc/images/pixel.gif"></td><td width="100%">
  <!-- top navigation -->
  <table width="100%" cellspacing="0" cellpadding="0" border="0" summary="">
  <tr class="toptitle"><td rowspan="2" width="90%" background="/resin-doc/images/hbleed.gif"><font class="toptitle" size="+3">
          &nbsp;
          Local Stateless Session Hello</font></td></tr>
  </table></td></tr>
<tr><td colspan="4"><img alt="" width="1" height="5" src="/resin-doc/images/pixel.gif"></td></tr>
<tr><td colspan="2" background="/resin-doc/images/left_background.gif"><img alt="" width="1" height="20" src="/resin-doc/images/pixel.gif"></td><td colspan="2"><img alt="" width="1" height="20" src="/resin-doc/images/pixel.gif"></td></tr>
<tr valign="top"><td bgcolor="#b9cef7"></td><td bgcolor="#b9cef7">
  <table cellspacing="0" cellpadding="2" border="0" width="100%">
  <tr><td><a class="leftnav" href="/resin-doc/index.xtp">Resin 3.0
</a></td></tr>
  <tr><td>
    <hr></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/features/index.xtp">Features
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/install/index.xtp">Installation
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/config/index.xtp">Configuration
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/webapp/index.xtp">Web Applications
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/ioc/index.xtp">IOC/AOP
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/resource/index.xtp">Resources
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/jsp/index.xtp">JSP
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/servlet/index.xtp">Servlets and Filters
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/portlet/index.xtp">Portlets
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/db/index.xtp">Databases
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/jmx/index.xtp">Admin (JMX)
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/cmp/index.xtp">CMP
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/ejb/index.xtp">EJB
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/amber/index.xtp">Amber
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/ejb3/index.xtp">EJB 3.0
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/security/index.xtp">Security
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/xml/index.xtp">XML and XSLT
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/xtp/index.xtp">XTP
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/jms/index.xtp">JMS
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/performance/index.xtp">Performance
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/protocols/index.xtp">Protocols
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/thirdparty/index.xtp">Third-party
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/troubleshoot/index.xtp">Troubleshooting/FAQ
</a></td></tr>
  <tr><td>
    <hr></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/ejb3/bean-ann.xtp">Bean Annotation
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/ejb3/cmp-table.xtp">Table
</a></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/ejb3/tutorial/index.xtp">Tutorials
</a></td></tr>
  <tr><td>
    <hr></td></tr>
  <tr><td><a class="leftnav" href="/resin-doc/ejb3/tutorial/stateless/index.xtp">Stateless Session
</a></td></tr>
  </table></td><td width="10"><img alt="" width="10" height="1" src="/resin-doc/images/pixel.gif"></td><td>
  <table border="0" cellspacing="0" width="100%">
  <tr><td><a href="/resin-doc/ejb3/tutorial/index.xtp">Tutorials</a></td><td width="100%">
    <center><a href="/resin-doc/ejb3/tutorial/index.xtp">Tutorials</a></center></td><td align="right"><a href="/resin-doc/security/index.xtp">Security</a></td></tr>
  </table>
  <hr>Find this tutorial in: <code>/Users/ferg/ws/resin/doc/ejb3/tutorial/stateless</code>
  <br><a href="stateless">Try the Tutorial</a><hr>
  <p>Stateless sessions make database queries and updates robust by
setting transaction boundaries at each business method.
This <span class="meta">stateless session</span> bean example annotates a single
business method with a SUPPORTS transaction attribute, marking the
method as a read-only transaction boundary.</p>
  <p>See also:</p>
  <ul>
  <li>The <a href="../resin-xa/index.xtp">Resin Transaction</a> example for a more detailed description of method transaction enhancement.
  
  <li>The <a href="../cmp-xa/index.xtp">CMP Transaction</a> example for a traditional session bean pattern.
  
  <li>The <a class="doc" href="/resin-doc/ejb3/bean-ann.xtp#@TransactionAttribute">@TransactionAttribute</a>  reference.

  </ul>
  <p>
  <center>
    <table width="90%" class="toc" border="3">
    <tr><td>
      <ol>
        <li><a href="#Files-in-this-tutorial">Files in this tutorial</a>
        <li><a href="#Local-Interface">Local Interface</a>
        <li><a href="#Bean-Implementation">Bean Implementation</a><ol>
            <li><a href="#@Stateless">@Stateless</a>
            <li><a href="#@Inject">@Inject</a>
            <li><a href="#Alternate-Dependency-Injection">Alternate Dependency Injection</a>
            <li><a href="#@TransactionAttribute">@TransactionAttribute</a>
          </ol>
        <li><a href="#Configuring-the-Resin-EJB-server">Configuring the Resin EJB server</a>
        <li><a href="#Client">Client</a><ol>
            <li><a href="#@EJB">@EJB</a>
          </ol>
      </ol></td></tr>
    </table>
  </center>
  <p>
  <p>A Hello, World example for EJB 3.0 is much simpler than for earlier
versions of EJB.  To implement the EJB you need to implement:</p>
  <ul>
  <li>A local interface

  <li>The bean implementation

  </ul>
  <p>To configure Resin to be a server for the EJB you need to:</p>
  <ul>
  <li>Configure the ejb-server

  <li>Configure the client

  </ul>
  <p>In this tutorial, a simple "Hello" EJB is created and
deployed within Resin.</p>
  <p>
  <table border="0" cellpadding="5" cellspacing="0" width="100%">
  <tr class="section"><td><font size="+2"><b><a name="Files-in-this-tutorial">Files in this tutorial</a></b></font></td></tr>
  </table>
  <p>
  <table width="90%" cellpadding="2" cellspacing="0" class="deftable" border="">
  <tr><td><code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fweb.xml&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/web.xml</a></code>
</td><td>web.xml configuration
</td></tr>
  <tr><td><code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fclasses%2Fexample%2FHello.java&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/classes/example/Hello.java</a></code>
</td><td>The local interface for the stateless session bean
</td></tr>
  <tr><td><code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fclasses%2Fexample%2FHelloBean.java&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/classes/example/HelloBean.java</a></code>
</td><td>The implementation for the stateless session bean
</td></tr>
  <tr><td><code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fclasses%2Fexample%2FHelloServlet.java&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/classes/example/HelloServlet.java</a></code>
</td><td>The client for the stateless session bean
</td></tr>
  </table>
  <p>
  <table border="0" cellpadding="5" cellspacing="0" width="100%">
  <tr class="section"><td><font size="+2"><b><a name="Local-Interface">Local Interface</a></b></font></td></tr>
  </table>
  <p>The remote interface defines the client view of the bean.
It declares all the business methods.  Our
only business method is the <tt>hello</tt> method.</p>
  <p>
  <table class="egpad" cellspacing="0" width="90%">
  <caption><font size="+1">Hello.java<br>See it in: <code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fclasses%2Fexample%2FHello.java&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/classes/example/Hello.java</a></code></font>
  </caption>
  <tr><td class="example" bgcolor="#ffeecc">
    <pre>
package example;

public interface Hello {
  public String hello();
}
</pre></td></tr>
  </table>
  <p>
  <table border="0" cellpadding="5" cellspacing="0" width="100%">
  <tr class="section"><td><font size="+2"><b><a name="Bean-Implementation">Bean Implementation</a></b></font></td></tr>
  </table>
  <p>The second class for EJBs is the bean implementation class.  It implements
the functionality provided by the remote interface.</p>
  <p>
  <table class="egpad" cellspacing="0" width="90%">
  <caption><font size="+1">HelloBean.java<br>See it in: <code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fclasses%2Fexample%2FHelloBean.java&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/classes/example/HelloBean.java</a></code></font>
  </caption>
  <tr><td class="example" bgcolor="#ffeecc">
    <pre>
package example;

import static javax.ejb.TransactionAttributeType.SUPPORTS;

@javax.ejb.Stateless
public class HelloBean implements Hello {
  private String _greeting = "Default Hello";

  <a class="doc" href="/resin-doc/ejb3/inject.xtp#@Inject">@javax.ejb.Inject</a> 
  public void setGreeting(String greeting)
  {
    _greeting = greeting;
  }

  @javax.ejb.TransactionAttribute(SUPPORTS)
  public String hello()
  {
    return _greeting;
  }
}
</pre></td></tr>
  </table>
  <h3><a name="@Stateless">@Stateless</a></h3>
  <p>The @Stateless annotation marks the bean as a stateless session
bean.  Resin will create a stub implementing <tt>Hello</tt> and
store it in JNDI at "java:comp/env/ejb/HelloBean".</p>
  <p>The @Stateless annotation can have an optional <tt>name</tt>
value which overrides the default name of "HelloBean".</p>
  <h3><a name="@Inject">@Inject</a></h3>
  <p>The <a class="doc" href="/resin-doc/ejb3/inject.xtp#@Inject">@javax.ejb.Inject</a> 
annotation tells Resin to lookup the greeting from JNDI when the
session bean is created.  The JNDI name will be
java:comp/env/greeting.</p>
  <p>In this example, the greeting is configured with an &lt;env-entry>
in the web.xml.</p>
  <h3><a name="Alternate-Dependency-Injection">Alternate Dependency Injection</a></h3>
  <p>The EJB 3.0 draft spec's dependency injection is somewhat
inflexible since the greeting is required to be in JNDI.  Resin
offers a more flexible dependency injection configuration based
on the configuration file.  By setting the value in the configuration
file, Resin's alternate dependency injection adds more flexibility and
some clarity.</p>
  <p>
  <table class="egpad" cellspacing="0" width="90%">
  <tr><td class="example" bgcolor="#ffeecc">
    <pre>
&lt;ejb-server jndi-name="java:comp/env/ejb">
  &lt;bean type="qa.TestBean">
    &lt;init greeting="Hello, World from web.xml"/>
  &lt;/bean>
&lt;/ejb-server>
</pre></td></tr>
  </table>
  <h3><a name="@TransactionAttribute">@TransactionAttribute</a></h3>
  <p>Managing transactions is the primary purpose of stateless
session beans.  Transactions are a more powerful version of
a <tt>synchronized</tt> lock used to protect database integrity.
<a class="doc" href="/resin-doc/ejb3/bean-ann.xtp#@TransactionAttribute">@TransactionAttribute</a> 
marks the transaction boundary for each business method.</p>
  <p>
  <table class="egpad" cellspacing="0" width="90%">
  <tr><td class="example" bgcolor="#ffeecc">
    <pre>
@javax.ejb.TransactionAttribute(SUPPORTS)
public String hello()
</pre></td></tr>
  </table>
  <p>The <tt>hello()</tt> business method uses SUPPORTS because it's
a read-only method.  It doesn't need to start a new transaction on its
own, but will participate in any transaction that already exists.</p>
  <p>The REQUIRED transaction value starts up a new transaction if none
already exists.  It's used when updating database values.</p>
  <p>
  <table width="90%" cellpadding="2" cellspacing="0" class="deftable" border="">
  <tr><th>TransactionAttribute</th><th>meaning
</th></tr>
  <tr><td>REQUIRED</td><td>Start a new transaction if necessary
</td></tr>
  <tr><td>SUPPORTS</td><td>Don't start a new transaction, but use one if it
exists
</td></tr>
  </table>
  <p>
  <table border="0" cellpadding="5" cellspacing="0" width="100%">
  <tr class="section"><td><font size="+2"><b><a name="Configuring-the-Resin-EJB-server">Configuring the Resin EJB server</a></b></font></td></tr>
  </table>
  <p><tt>&lt;ejb-server&gt;</tt> configure the Resin EJB server.
Typically it configures the EJB root using jndi-name and configures
a number of EJB classes using &lt;bean>.  The &lt;bean> entry will
look at the bean's annotations to enhance the class.</p>
  <p>
  <table class="egpad" cellspacing="0" width="90%">
  <caption><font size="+1">ejb-server in web.xml<br>See it in: <code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fweb.xml&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/web.xml</a></code></font>
  </caption>
  <tr><td class="example" bgcolor="#ffeecc">
    <pre>
&lt;web-app xmlns="http://caucho.com/ns/resin"&gt;

  ...
  &lt;env-entry env-entry-name="greeting"
             env-entry-type="java.lang.String"
             env-entry-value="Hello, World."/>

  &lt;ejb-server jndi-name="java:comp/env/ejb">
    &lt;bean type="qa.TestBean"/>
  &lt;/ejb-server>
    
  ...

&lt;/web-app&gt;
</pre></td></tr>
  </table>
  <p>The &lt;bean> can optionally configure the bean instances with an
&lt;init> tag as described in the alternate dependency injection section.</p>
  <p>
  <table border="0" cellpadding="5" cellspacing="0" width="100%">
  <tr class="section"><td><font size="+2"><b><a name="Client">Client</a></b></font></td></tr>
  </table>
  <p>
  <table class="egpad" cellspacing="0" width="90%">
  <caption><font size="+1">See it in: <code class="viewfile"><a class="viewfile" href="/resin-doc/viewfile/?contextpath=%2Fresin-doc%2Fejb3%2Ftutorial%2Fstateless&servletpath=%2Findex.xtp&file=WEB-INF%2Fclasses%2Fexample%2FHelloServlet.java&re-marker=&re-start=&re-end=#code-highlight">WEB-INF/classes/example/HelloServlet.java</a></code></font>
  </caption>
  <tr><td class="example" bgcolor="#ffeecc">
    <pre>
public class HelloServlet extends GenericServlet {
  private Hello _hello;

  @javax.ejb.EJB
  public void setHello(Hello hello)
  {
    _hello = hello;
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();
    
    out.println(_hello.hello());
  }
}
</pre></td></tr>
  </table>
  <h3><a name="@EJB">@EJB</a></h3>
  <p>The <a class="doc" href="/resin-doc/ejb3/cmp-inject.xtp#@EJB">@EJB</a>  annotation tells
Resin to lookup the session bean in JNDI with name
"java:comp/env/ejb/HelloBean".</p>
  <p>The servlet could also lookup the Hello bean with JNDI in the
<tt>init()</tt> method or use an &lt;init> configuration in the
web.xml:

    <p>
    <table class="egpad" cellspacing="0" width="90%">
    <caption><font size="+1">alternative configuration</font>
    </caption>
    <tr><td class="example" bgcolor="#ffeecc">
      <pre>
&lt;servlet servlet-name="hello" servlet-class="example.HelloServlet">
  &lt;init hello="\${jndi:lookup('java:comp/env/ejb/HelloBean')}"/>
&lt;/servlet>
</pre></td></tr>
    </table>
  </p>
  <p><a href="stateless">Try the Tutorial</a></p>
  <hr>
  <table border="0" cellspacing="0" width="100%">
  <tr><td><a href="/resin-doc/ejb3/tutorial/index.xtp">Tutorials</a></td><td width="100%">
    <center><a href="/resin-doc/ejb3/tutorial/index.xtp">Tutorials</a></center></td><td align="right"><a href="/resin-doc/security/index.xtp">Security</a></td></tr>
  </table>
  <table border="0" cellspacing="0" width="100%">
  <tr><td><em><small>Copyright &copy; 1998-2005 Caucho Technology, Inc. All rights reserved.<br>
Resin<sup><font size="-1">®</font></sup> is a registered trademark,
and HardCore<sup>tm</sup> and Quercus<sup>tm</sup> are trademarks of Caucho Technology, Inc.</small></em></td><td align="right"><img width="96" height="32" src="/resin-doc/images/logo.gif"></td></tr>
  </table></td></tr>
</table>
</body>
</html>
