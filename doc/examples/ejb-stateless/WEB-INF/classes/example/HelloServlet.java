package example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {
  // Dependency injection for the hello bean
  @Inject
  private Hello _hello;

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();
    
    out.println(_hello.hello());
  }
}
