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
using System.Diagnostics;
using System.Net.Sockets;

namespace Caucho.IIS
{
  public class Server
  {
    private Logger _log;

    private String _serverInternalId;

    private HmuxConnection[] _idle = new HmuxConnection[64];
    private volatile int _idleHead;
    private volatile int _idleTail;
    private int _idleSize = 16;

    private int _maxConnections = int.MaxValue / 2;

    private int _loadBalanceIdleTime = 10000;
    private volatile int _keepaliveCountTotal;

    private volatile int _activeCount;
    private volatile int _startingCount;

    private State _state;

    private String _address;
    private int _port;

    public Server(String serverInternalId, String address, int port)
    {
      _serverInternalId = serverInternalId;
      _address = address;
      _port = port;

      _log = Logger.GetLogger();

      _state = State.ACTVIE;
    }

    public bool IsActive()
    {
      return _state == State.ACTVIE;
    }

    public HmuxConnection OpenServer()
    {
      HmuxConnection channel = OpenRecycle();

      if (channel != null)
        return channel;

      return Connect();
    }


    private HmuxConnection OpenRecycle()
    {
      long now = DateTime.Now.Ticks;
      HmuxConnection channel = null;

      lock (this) {
        if (_idleHead != _idleTail) {
          channel = _idle[_idleHead];
          long freeTime = channel.GetIdleStartTime();

          _idle[_idleHead] = null;
          _idleHead = (_idleHead + _idle.Length - 1) % _idle.Length;

          if (now < freeTime + _loadBalanceIdleTime) {
            _activeCount++;
            _keepaliveCountTotal++;

            channel.ClearIdleStartTime();
            channel.ToActive();

            Trace.TraceInformation("OpenRecycle '{0}'", channel);

            return channel;
          }
        }
      }

      if (channel != null) {
        if (_log.IsLoggable(EventLogEntryType.Information))
          _log.Info(this + " close idle " + channel
                    + " expire=" + new DateTime(channel.GetIdleStartTime() + _loadBalanceIdleTime));

        channel.CloseImpl();
      }

      Trace.TraceInformation("OpenRecyle return 'null'");
      return null;
    }

    private HmuxConnection Connect()
    {
      lock (this) {
        if (_maxConnections <= _activeCount + _startingCount)
          return null;

        _startingCount++;
      }

      try {
        Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        socket.Connect(_address, _port);
        HmuxConnection channel = new HmuxConnection(socket, this, _serverInternalId);
        
        lock (this) {
          _activeCount++;
        }

        if (_log.IsLoggable(EventLogEntryType.Information))
          _log.Info("Connect " + channel);

        Trace.TraceInformation("Connect '{0}'", channel);

        return channel;
      } catch (Exception e) {
        _log.Info("Can't create HmuxChannel to '{0}:{1}' due to: {2} \t {3}", _address, _port, e.Message, e.StackTrace);

        FailConnect();

        return null;
      } finally {
        lock (this) {
          _startingCount--;
        }
      }
    }


    public void FailConnect()
    {
      lock (this) {
        _state = State.INACTIVE;
      }
    }

    internal void Busy()
    {
      Trace.TraceInformation("HmuxChannelFactory.Busy() NYI");
    }

    internal void FailSocket()
    {
      _state = State.INACTIVE;
    }

    internal void SetCpuLoadAvg(double loadAvg)
    {
      Trace.TraceInformation("HmuxChannelFactory.SetCpuLoadAvg() NYI");
    }

    internal void Success()
    {
      Trace.TraceInformation("HmuxChannelFactory.SetCpuLoadAvg() NYI");
    }

    internal void Free(HmuxConnection channel)
    {
      Success();

      lock (this) {
        _activeCount--;

        int size = (_idleHead - _idleTail + _idle.Length) % _idle.Length;

        if (_state != State.INACTIVE && size < _idleSize) {
          _idleHead = (_idleHead + 1) % _idle.Length;
          _idle[_idleHead] = channel;

          channel = null;
        }
      }

      long now = DateTime.Now.Ticks;
      long maxIdleTime = _loadBalanceIdleTime;
      HmuxConnection oldChannel = null;

      do {
        oldChannel = null;

        lock (this) {
          if (_idleHead != _idleTail) {
            int nextTail = (_idleTail + 1) % _idle.Length;

            oldChannel = _idle[nextTail];

            if (oldChannel != null
                && oldChannel.GetIdleStartTime() + maxIdleTime < now) {
              _idle[nextTail] = null;
              _idleTail = nextTail;
            } else
              oldChannel = null;
          }
        }

        if (oldChannel != null)
          oldChannel.CloseImpl();
      } while (oldChannel != null);

      if (channel != null) {
        channel.CloseImpl();
      }
    }

    internal void Close(HmuxConnection channel)
    {
      if (_log.IsLoggable(EventLogEntryType.Information))
        _log.Info("Close {0}", channel);

      Trace.TraceInformation("Close '{0}'", channel);

      lock (this) {
        _activeCount--;
      }
    }

    internal void Close()
    {
      if (_log.IsLoggable(EventLogEntryType.Information))
        _log.Info("Close {0}", this);

      Trace.TraceInformation("Close '{0}'", this);

      lock (this) {
        if (_state == State.INACTIVE)
          return;

        _state = State.INACTIVE;
        _idleHead = _idleTail = 0;
      }

      for (int i = 0; i < _idle.Length; i++) {
        HmuxConnection channel;

        lock (this) {
          channel = _idle[i];
          _idle[i] = null;
        }

        if (channel != null)
          channel.CloseImpl();
      }
    }

    internal string GetDebugId()
    {
      return _address + _port.ToString();
    }

    public override string ToString()
    {
      String format = this.GetType().Name + "'{0}:{1}:{2}'";
      return String.Format(format, _serverInternalId, _address, _port);
    }
  }

  enum State
  {
    ACTVIE,
    INACTIVE
  }
}