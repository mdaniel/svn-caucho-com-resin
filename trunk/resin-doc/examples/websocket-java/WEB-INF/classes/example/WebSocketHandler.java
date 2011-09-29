package example;

import javax.servlet.*;
import java.io.*;
import java.util.logging.*;

import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.AbstractWebSocketListener;

public class WebSocketHandler extends AbstractWebSocketListener {
  private static final Logger log
    = Logger.getLogger(WebSocketHandler.class.getName());

  public void onStart(WebSocketContext context)
    throws IOException
  {
    // sets the connection timeout to 120s
    context.setTimeout(120000);
  }

  public void onReadBinary(WebSocketContext context, InputStream is)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    int ch;

    while ((ch = is.read()) >= 0) {
      sb.append((char) ch);
    }

    String message = sb.toString();
    String result = "unknown message";

    if ("hello".equals(message))
      result = "world";
    else if ("server".equals(message))
      result = "Resin";

    OutputStream os = context.startBinaryMessage();

    os.write(result.getBytes("utf-8"));

    os.close();
  }

  public void onComplete(WebSocketContext context)
    throws IOException
  {
  }

  public void onTimeout(WebSocketContext context)
    throws IOException
  {
  }
}