/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;
using System.Diagnostics;

namespace Caucho.IIS
{
  public class LoadBalancer
  {
    private Logger _log;
    private Server[] _servers;
    private Random _random;
    private volatile int _roundRobinIdx;

    //supports just one server for now
    public LoadBalancer(String servers)
    {
      _log = Logger.GetLogger();

      List<Server> pool = new List<Server>();
      String[] sruns = servers.Split(new char[] { ',', ' ' }, StringSplitOptions.RemoveEmptyEntries);

      for (int i = 0; i < sruns.Length; i++) {
        String server = sruns[i];
        int portIdx = server.LastIndexOf(':');
        String address = server.Substring(0, portIdx);
        int port = int.Parse(server.Substring(portIdx + 1, server.Length - portIdx - 1));
        char c = (char)('a' + i);
        _log.Info("Adding Server '{0}:{1}:{2}'", c, address, port);
        pool.Add(new Server("a", address, port));
      }

      _servers = pool.ToArray();

      _random = new Random();      
    }

    public void Init()
    {
    }

    public HmuxConnection OpenServer(String sessionId, Server xChannelFactory)
    {
      Trace.TraceInformation("{0}:{1}", _servers.Length, _servers[0]);

      HmuxConnection connection = null;
      if (sessionId != null)
        connection = OpenSessionServer(sessionId);

      if (connection == null)
        connection = OpenAnyServer(xChannelFactory);

      return connection;
    }

    public HmuxConnection OpenSessionServer(String sessionId)
    {
      char c = sessionId[0];

      Server server = _servers[(c - 'a')];

      HmuxConnection connection = null;

      try {
        if (server.IsActive())
          connection = server.OpenServer();
      } catch (Exception e) {
        if (_log.IsLoggable(EventLogEntryType.Information))
          _log.Info("Error openning session server '{0}'\t {1}", e.Message, e.StackTrace);
      }

      return connection;
    }

    public HmuxConnection OpenAnyServer(Server xChannelFactory)
    {
      int serverCount = _servers.Length;

      Server server = null;
      HmuxConnection connection = null;

      int id = 0;

      lock (this) {
        _roundRobinIdx = _roundRobinIdx % serverCount;
        id = _roundRobinIdx;
        _roundRobinIdx++;
      }

      try {
        server = _servers[id];
        if (server.IsActive())
          connection = server.OpenServer();
      } catch (Exception e) {
        server.Close();
      }

      if (connection != null)
        return connection;

      lock (this) {
        _roundRobinIdx = _random.Next(serverCount);
        for (int i = 0; i < serverCount; i++) {
          id = (i + _roundRobinIdx) % serverCount;
          server = _servers[id];
          try {
            if (xChannelFactory != server && server.IsActive()) {
              connection = server.OpenServer();
            }
          } catch (Exception e) {
            server.Close();
          }
        }
      }

      return connection;
    }

    public void Destroy()
    {
      foreach (Server factory in _servers) {
        factory.Close();
      }
    }
  }
}
