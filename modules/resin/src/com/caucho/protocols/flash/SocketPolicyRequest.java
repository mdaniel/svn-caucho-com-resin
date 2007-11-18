package com.caucho.protocols.flash;

import java.io.*;

import com.caucho.server.connection.Connection;
import com.caucho.server.port.ServerRequest;

public class SocketPolicyRequest implements ServerRequest
{
  private final ByteArrayOutputStream _policy;
  private final Connection _connection;

  public SocketPolicyRequest(ByteArrayOutputStream policy,
                             Connection connection)
  {
    _policy = policy;
    _connection = connection;
  }

  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
  }

  /**
   * Return true if the connection should wait for a read before
   * handling the request.
   */
  public boolean isWaitForRead()
  {
    return true;
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   */
  public boolean handleRequest() 
    throws IOException
  {
    if (_policy == null)
      return false;
    
    OutputStream out = _connection.getWriteStream();
    _policy.writeTo(out);
    out.write(0); // null byte required
    out.flush();

    return false;
  }
  
  /**
   * Resumes processing after a wair.
   */
  public boolean handleResume() 
    throws IOException
  {
    return false;
  }

  /**
   * Handles a close event when the connection is closed.
   */
  public void protocolCloseEvent()
  {
  }
}
