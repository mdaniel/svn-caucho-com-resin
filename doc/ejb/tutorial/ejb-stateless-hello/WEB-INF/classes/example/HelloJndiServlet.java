package example;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Client servlet that:
 * <ol>
 * <li>uses JNDI to get a HessianContextFactory
 * <li>uses the factory to obtain a HelloHome
 * <li>uses the HelloHome to create a Home stub
 * <li>uses the Home stub to query a server about a greeting.
 * </ol>
 */
public class HelloJndiServlet extends HttpServlet {
  static protected final Logger log = 
    Logger.getLogger(HelloJndiServlet.class.getName());

  /**
   * The servlet stores the Home after the initial lookup.
   * Since the Home never changes, caching the lookup in 
   * a member variable saves some performance.
   */
  private HelloHome helloHome;

  /**
   * The init method looks up the HelloHome using JNDI and
   * stores it in a member variable.
   */
  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);

    /**
     * Connect to the remote EJB server using JNDI and obtain a HomeHandle.
     */
    try {
      Context ejb = (Context) new InitialContext().lookup("java:comp/env/ejb");

      /*
       * Look up the hello home.  "hello" is the ejb-name
       * in the deployment descriptor.
       */
      helloHome = (HelloHome) ejb.lookup("hello");

    } catch (NamingException e) {
      throw new ServletException("java:comp/env/ejb",e);
    }
  }

  /**
   * Call the hello method.  The Hello stub is created using the
   * create method of HelloHome.
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

      Hello hello = helloHome.create();

      out.print("Message: ");
      out.println(hello.hello());
    } catch (CreateException e) {
      throw new ServletException(e);
    }
  }
}
