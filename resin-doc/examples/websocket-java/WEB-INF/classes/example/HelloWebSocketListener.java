package example;

import javax.servlet.*;
import java.io.*;
import java.util.logging.*;

import com.caucho.websocket.*;

public class HelloWebSocketListener extends AbstractWebSocketListener {
    @Override
    public void onStart(WebSocketContext context)
      throws IOException
    {
      // called when the connection starts
    }

    public void onReadText(WebSocketContext context, Reader is)
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
      else
        result = "unknown command";

      PrintWriter out = context.startTextMessage();

      out.println(result);

      out.close();

      is.close();
    }

    public void onClose(WebSocketContext context)
      throws IOException
    {
	// called when the client closes gracefully
    }

    public void onDisconnect(WebSocketContext context)
      throws IOException
    {
	// called when the client closes disconnects
    }
}