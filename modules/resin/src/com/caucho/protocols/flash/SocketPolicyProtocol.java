package com.caucho.protocols.flash;

import javax.annotation.*;
import java.io.*;

import com.caucho.config.*;
import com.caucho.server.cluster.Server;
import com.caucho.server.connection.Connection;
import com.caucho.server.port.Protocol;
import com.caucho.server.port.ServerRequest;
import com.caucho.util.*;
import com.caucho.vfs.Path;

/**
 * Simple protocol that sends the contents of a specified file as soon
 * as it is contacted.  It is intended for sending flash policy files
 * for flash.net.Sockets when the target port of the socket is < 1024.
 *
 **/
public class SocketPolicyProtocol extends Protocol
{
  private final static L10N L = new L10N(SocketPolicyRequest.class);
  
  private String _protocolName = "http";
  private Path _policy;

  public void setSocketPolicyFile(Path path)
  {
    setPolicyFile(path);
  }

  /**
   * Sets the flash socket policy file.
   */
  public void setPolicyFile(Path path)
  {
    _policy = path;
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

  @PostConstruct
  public void init()
  {
    if (_policy == null)
      throw new ConfigException(L.l("flash requires a policy-file"));
  }

  public ServerRequest createRequest(Connection conn)
  {
    return new SocketPolicyRequest((Server) getServer(), conn, _policy);
  }
}
