package example;

import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.io.*;

@WebServlet("/hello-servlet")
public class HelloServlet extends HttpServlet
{
  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws IOException, ServletException
  {
    PrintWriter out = response.getWriter();

    response.setContentType("text/html");

    out.println("<h1>hello, world</h1>");
  }
}
