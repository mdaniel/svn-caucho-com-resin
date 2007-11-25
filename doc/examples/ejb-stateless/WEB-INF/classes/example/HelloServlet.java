package example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.webbeans.In;

public class HelloServlet extends HttpServlet {
  // Dependency injection for the hello bean
  @In private Hello _hello;

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();
    
    out.println(_hello.hello());
  }
}
