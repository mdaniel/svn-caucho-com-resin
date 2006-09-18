package example;

import java.io.*;
import javax.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ServiceFrontendServlet extends HttpServlet {
  @Resource(name="hessian/HelloService")
  private HelloService _helloService;

  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws IOException, ServletException
  {
    PrintStream out = new PrintStream(resp.getOutputStream());

    out.println("service result: " + _helloService.hello());
  }
}

