package example.session.local;

import java.io.*;

import javax.ejb.*;
import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Client servlet querying a server about a greeting.
 *
 * <h3>JNDI client configuration</h3>
 *
 * <code><pre>
 * &lt;jndi-link>
 *   &lt;jndi-name>java:comp/env/ejb&lt;/jndi-name>
 *   &lt;jndi-factory>com.caucho.ejb.BurlapContextFactory&lt;/jndi-factory>
 *   &lt;init-param java.naming.provider.url="http://localhost:8080/cmp/example"/>
 * &lt;/jndi-link>
 * </pre></code>
 */
public class CounterServlet extends HttpServlet {
  /**
   * The servlet stores the home interface after the initial lookup.
   * Since the home interface never changes, caching the lookup will save
   * some performance.
   */
  private CounterHome counterHome;

  /**
   * The init method looks up the CounterHome interface using JNDI and
   * stores it in a servlet variable.
   */
  public void init()
    throws ServletException
  {
    try {
      /*
       * Look up the local EJB context using JNDI.
       * The context contains the EJB beans in the local web-app
       *
       * Since we're using the remote interface, it uses ejb.
       */
      Context ejb = (Context) new InitialContext().lookup("java:comp/env/ejb");
      /*
       * Look up the counter home.  "session-counter" is the ejb-name
       * in the deployment descriptor.
       */
      counterHome = (CounterHome) ejb.lookup("session-counter");
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Calls the counter twice.  Since it's a stateful session bean, the
   * counter is new for each call.
   *
   * @param request the servlet request object.
   * @param response the servlet response object.
   */
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
    throws IOException, ServletException
  {
    try {
      PrintWriter out = response.getWriter();

      response.setContentType("text/html");

      Counter counter = counterHome.create();

      out.print("Count: ");
      out.print(counter.hit());
      out.println("<br>");
      
      out.print("Count: ");
      out.print(counter.hit());
      out.println("<br>");
    } catch (CreateException e) {
      throw new ServletException(e);
    }
  }
}
