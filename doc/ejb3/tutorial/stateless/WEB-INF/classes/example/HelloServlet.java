package example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ejb.EJB;

public class HelloServlet extends HttpServlet {
  private Hello _hello;

  /**
   * Dependency injector for the Hello interface.
   */
  @EJB(name="HelloBean")
  public void setHello(Hello hello)
  {
    _hello = hello;
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    if (_hello == null) {
      out.println("This example requires JDK 1.5");
      return;
    }
    
    out.println(_hello.hello());
  }
}
