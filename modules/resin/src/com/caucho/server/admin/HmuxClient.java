/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * @author Sam
 */

package com.caucho.server.admin;

import com.caucho.config.ConfigException;
import com.caucho.hessian.io.ExtSerializerFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterPort;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.ClusterStream;
import com.caucho.server.cluster.ServerPool;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;

public class HmuxClient
{
  private static final L10N L = new L10N(HmuxClient.class);

  private final ServerPool _client;

  private ExtSerializerFactory _extFactory;

  public HmuxClient(String serverId)
  {
    _client = findClient(serverId);

    if (_client == null)
      throw new ConfigException(L.l("'{0}' is an unknown server.",
                                    serverId));
  }

  public HmuxClient(String host, int port)
  {
    _client = createClient(host, port);
  }

  /**
   * Finds the ClusterServer in the current Resin instances by its
   * server-id and returns its ServerPool.
   */
  private ServerPool findClient(String serverId)
  {
    Resin resin = Resin.getCurrent();

    if (resin == null)
      throw new ConfigException(L.l("No active Resin is available."));

    ClusterServer server = resin.findClusterServer(serverId);

    if (server == null)
      throw new ConfigException(L.l("'{0}' is an unknown server.",
                                    serverId));

    return server.getServerPool();
  }

  /**
   * Creates a new ServerPool to a Resin server by the address and port.
   */
  private ServerPool createClient(String address, int port)
  {
    try {
      ServerPool conn = new ServerPool("hmux", address + ":" + port,
				       address, port, false);

      conn.init();

      return conn;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Object queryGet(String to, Serializable query)
  {
    ClusterStream stream = null;

    boolean isQuit = false;
    try {
      stream = _client.open();

      if (stream == null)
	throw new IOException(L.l("Can't connect to '{0}'", _client));

      long id = 0;

      stream.queryGet(id, to, "", query);

      Object result = stream.readQueryResult(id);

      int code = stream.getReadStream().read();

      if (code == 'Q')
	isQuit = true;
      else if (code != 'X')
	throw new IllegalStateException("unexpected code " + (char) code);

      return result;
    } catch (Exception e) {
      isQuit = false;
      
      throw ConfigException.create(e);
    } finally {
      if (stream == null) {
      }
      else if (isQuit)
	stream.free();
      else
	stream.close();
    }
  }

  public Object querySet(String to, Serializable query)
  {
    ClusterStream stream = null;
    boolean isQuit = false;
    
    try {
      stream = _client.open();

      long id = 0;

      stream.querySet(id, to, "", query);

      Object result = stream.readQueryResult(id);

      int code = stream.getReadStream().read();

      if (code == 'Q')
	isQuit = true;
      else if (code != 'X')
	throw new IllegalStateException("unexpected code " + (char) code);

      return result;
    } catch (Exception e) {
      isQuit = false;
      
      throw ConfigException.create(e);
    } finally {
      if (stream == null) {
      }
      else if (isQuit)
	stream.free();
      else
	stream.close();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _client + "]";
  }
}

