/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
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
import com.caucho.server.cluster.ServerConnector;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

abstract public class HmuxClient
{
  private L10N L = new L10N(HmuxClient.class);

  private final String _serviceName;
  private final ServerConnector _client;

  private ExtSerializerFactory _extFactory;

  protected HmuxClient(String serviceName, String serverId)
  {
    _serviceName = serviceName;

    _client = findClient(serverId);

    if (_client == null)
      throw new ConfigException(L.l("'{0}' is an unknown server.",
                                    serverId));
    initHessian();
  }

  public String getServiceName()
  {
    return _serviceName;
  }

  protected HmuxClient(String serviceName, String host, int port)
  {
    _serviceName = serviceName;

    _client = createClient(host, port);

    initHessian();
  }

  private void initHessian()
  {
    _extFactory = new ExtSerializerFactory();

    initExtSerializerFactory(_extFactory);
  }

  abstract protected void initExtSerializerFactory(ExtSerializerFactory factory);

  private ServerConnector findClient(String serverId)
  {
    Resin resin = Resin.getLocal();

    if (resin == null)
      throw new ConfigException(L.l("No active Resin is available."));

    ClusterServer server = resin.findClusterServer(serverId);

    if (server == null)
      throw new ConfigException(L.l("'{0}' is an unknown server.",
                                    serverId));

    return server.getServerConnector();
  }

  private ServerConnector createClient(String address, int port)
  {
    try {
      Cluster cluster = new Cluster();
      ClusterServer server = new ClusterServer(cluster);
      ClusterPort clusterPort = server.createClusterPort();
      clusterPort.setAddress(address);
      clusterPort.setPort(port);
      //server.setPort(clusterPort);
      server.init();

      ServerConnector conn = server.getServerConnector();

      conn.init();

      return conn;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private HmuxOutput createHmuxOutput()
  {
    return new HmuxOutput("/" + _serviceName);
  }

  public ServiceCall createServiceCall(String method)
    throws IOException
  {
    return new ServiceCall(method);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _client + "]";
  }

  protected class ServiceCall
  {
    private ClusterStream _stream;

    private HmuxOutput _hmuxOut;
    private Hessian2Output _out;

    private ServiceCall(String method)
      throws IOException
    {
      ClusterStream stream = _client.open();

      if (stream == null)
        throw new IOException(L.l("Can't connect to '{0}' on client '{1}'",
                                  _serviceName, _client));

      try {
        WriteStream os = stream.getWriteStream();

        _hmuxOut = createHmuxOutput();

        _hmuxOut.open(os);

        _out = new Hessian2Output(_hmuxOut);

        _out.startCall(method);

        _stream = stream;

        stream = null;
      }
      finally {
        if (stream != null)
          stream.close();
      }
    }

    public void writeString(String name)
      throws IOException
    {
      _out.writeString(name);
    }

    public void writeObject(Object[] obj)
      throws IOException
    {
      _out.writeObject(obj);
    }

    public void complete()
      throws IOException
    {
      complete(null);
    }

    public <T> T complete(Class<T> returnType)
      throws IOException
    {
      try {
        Hessian2Output out = _out;
        HmuxOutput hmuxOut = _hmuxOut;

        _out = null;
        _hmuxOut = null;

        out.completeCall();
        out.close();
        hmuxOut.close();

        ReadStream is = _stream.getReadStream();

        HmuxInput hmuxIn = new HmuxInput();

        if (! hmuxIn.open(is) || ! hmuxIn.getStatus().startsWith("200"))
          throw new IOException(L.l("Can't connect to '{0}' on client '{1}'.  Status '{2}'.",
                                    _serviceName, _client, hmuxIn.getStatus()));

        Hessian2Input in = new Hessian2Input(hmuxIn);
        in.findSerializerFactory().addFactory(_extFactory);

        in.startReply();

        T ret;

        if (returnType != null)
          ret = (T) in.readObject(returnType);
        else
          ret = null;

        in.completeCall();
        in.close();
        hmuxIn.close();

        return ret;
      }
      catch (RuntimeException ex) {
        throw ex;
      }
      catch (IOException ex) {
        throw ex;
      }
      catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }

    public void close()
    {
      if (_stream != null)
        _stream.close();
    }
  }
}
