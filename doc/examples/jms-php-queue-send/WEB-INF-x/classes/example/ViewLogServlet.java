package example;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.webbeans.*;

public class ViewLogServlet extends HttpServlet {
  @In private MessageStoreService _messageStore;

  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    PrintWriter out = response.getWriter();

    out.println("<html>");
    out.println("<body>");
    out.println("<ul>");

    for (String messageString : _messageStore.getMessages()) {
      out.println("<li>" + messageString + "</li>");
    }

    out.println("<a href='index.xtp'>back to tutorial</a>");
    
    out.println("</ul>");
    out.println("</body>");
    out.println("</html>");
  }
}

