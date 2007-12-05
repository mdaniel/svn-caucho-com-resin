package example;

import java.util.logging.Logger;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

import javax.webbeans.In;

import com.caucho.services.message.MessageSender;

public class MessageServlet extends GenericServlet {
  private static final Logger log =
    Logger.getLogger(MessageServlet.class.getName());

  @In private MessageSender _sender;
  private int _count;
  
  /**
   * Sends the message.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    String message = "sample message: " + _count++;

    response.setContentType("text/html");

    PrintWriter out = response.getWriter();
    out.println("message: " + message + "<br>");
    
    log.info("sending: " + message);

    _sender.send(null, message);
    
    log.info("complete send");

    out.println("received message: " + MyListener.getLastMessage() + "<br>");
  }
}
