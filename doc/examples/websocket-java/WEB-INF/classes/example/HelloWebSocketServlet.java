package example;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.logging.*;

import com.caucho.websocket.*;

/**
 * The HelloWebSocketServlet validates the initial HTTP request and
 * dispatches a new WebSocket connection to the "hello" listener,
 * implemented by HelloWebSocketListener.
 */
public class HelloWebSocketServlet extends GenericServlet {
  private static final Logger log
    = Logger.getLogger(HelloWebSocketServlet.class.getName());

  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    /*
     * Process the handshake, selecting the protocol to be used. Since we
     * only understand "hello", return 404 for any known protocol.
     */

    String protocol = req.getHeader("Sec-WebSocket-Protocol");

    WebSocketListener listener;

    if ("hello".equals(protocol)) {
      listener = new HelloWebSocketListener();

      res.setHeader("Sec-WebSocket-Protocol", "hello");
    }
    else {
      res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return;
    }

    WebSocketServletRequest wsRequest = (WebSocketServletRequest) request;

    wsRequest.startWebSocket(listener);
  }
}