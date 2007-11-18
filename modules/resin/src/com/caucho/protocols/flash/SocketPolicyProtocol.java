package com.caucho.protocols.flash;

import java.io.*;

import com.caucho.server.connection.Connection;
import com.caucho.server.port.Protocol;
import com.caucho.server.port.ServerRequest;
import com.caucho.vfs.Vfs;

/**
 * Simple protocol that sends the contents of a specified file as soon
 * as it is contacted.  It is intended for sending flash policy files
 * for flash.net.Sockets when the target port of the socket is < 1024.
 *
 **/
public class SocketPolicyProtocol extends Protocol
{
  private String _protocolName = "socketPolicy";
  private ByteArrayOutputStream _policy = null;

  public void setSocketPolicyFile(String file)
    throws IOException
  {
    // XXX Make this dependency-based in case the file changes

    InputStream is = null;
    
    try {
      is = Vfs.openRead(file);
      _policy = new ByteArrayOutputStream();

      for (int ch = is.read(); ch >= 0; ch = is.read())
        _policy.write(ch);
    }
    finally {
      if (is != null)
        is.close();
    }
  }

  /**
   * Returns the protocol name.
   */
  public String getProtocolName()
  {
    return _protocolName;
  }
  
  /**
   * Sets the protocol name.
   */
  public void setProtocolName(String name)
  {
    _protocolName = name;
  }

  public ServerRequest createRequest(Connection connection)
  {
    return new SocketPolicyRequest(_policy, connection);
  }
}
