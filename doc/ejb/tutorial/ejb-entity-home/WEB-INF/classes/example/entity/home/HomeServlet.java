package example.entity.home;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.ejb.*;
import javax.naming.*;

/**
 * Client using the entity home call for a <code>hello</code> and
 * an <code>add</code> call.
 */
public class HomeServlet extends GenericServlet {
  /**
   * Caches the home interface from the JNDI lookup.
   */
  private Home home;

  /**
   * On init, load the Home from the JNDI context.
   */
  public void init()
    throws ServletException
  {
    try {
      /**
       * Because we're looking up the remote interface, the JNDI path
       * is java:comp/env/ejb.
       */
      Context ejb = (Context) new InitialContext().lookup("java:comp/env/ejb");

      /*
       * Find the home from JNDI.  "home" matches the ejb-name in the
       * home.ejb deployment descriptor.
       */
      home = (Home) ejb.lookup("home");
    } catch (Exception e) {
      throw new ServletException(e);
    }

    if (home == null)
      throw new ServletException("can't find home");
  }

  /**
   * Prints the result of the hello call and the add call.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    PrintWriter pw = response.getWriter();

    response.setContentType("text/html");
    
    try {
      pw.println("message: " + home.hello() + "<br>");
      
      pw.println("1 + 3 = " + home.add(1, 3) + "<br>");
      pw.println("7 + 1 = " + home.add(7, 1) + "<br>");
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
