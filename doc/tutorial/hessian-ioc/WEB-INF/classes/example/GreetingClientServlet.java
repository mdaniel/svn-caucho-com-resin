package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;

/**
 * The injection filter adds configuration objects to the request
 * attributes.
 */
public class GreetingClientServlet extends GenericServlet {
  private String _name = "generic";
  private GreetingAPI _greeting;

  /**
   * Sets the client servlet name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the client greeting.
   */
  public void setGreeting(GreetingAPI greeting)
  {
    _greeting = greeting;
  }

  /**
   * Servlet init
   */
  public void init()
    throws ServletException
  {
    if (_greeting == null)
      throw new ServletException("GreetingClientServlet needs a configured greeting");
  }

  /**
   * Runs the servlet
   */
  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    out.println(_name + ": " + _greeting.greeting());
  }
}
