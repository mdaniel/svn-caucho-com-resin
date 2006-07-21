package example;

import java.io.*;
import java.util.*;
import javax.jms.*;
import javax.ejb.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class JMSLogger extends HttpServlet
  implements MessageDrivenBean, MessageListener {

  private transient MessageDrivenContext _messageDrivenContext = null;
  private static LinkedList<String> _messageLog = new LinkedList<String>();

  public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext)
    throws EJBException
  {
    _messageDrivenContext = messageDrivenContext;
  }

  public void ejbCreate()
  {
  }

  public void onMessage(Message message)
  {
    try {
      synchronized (_messageLog) {
        if (message instanceof TextMessage)
          _messageLog.add(((TextMessage) message).getText());
        else
          _messageLog.add(message.getClass().getName());
      }
    } catch (JMSException e) {
      _messageDrivenContext.setRollbackOnly();
    }
  }

  public void ejbRemove()
  {
  }

  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    PrintWriter out = response.getWriter();
    ServletContext application = getServletContext();
    HttpSession session = request.getSession();

    try {
      out.println("<html>");
      out.println("<body>");
      out.println("<ul>");

      synchronized (_messageLog) {
        for (String messageString : _messageLog) {
          out.println("<li>" + messageString + "</li>");
        }
      }

      out.println("</ul>");
      out.println("</body>");
      out.println("</html>");
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}

