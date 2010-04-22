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
  public class HmuxChannelFactory
  {
    private Logger _log;
    private ActiveProbe _idleProbe;
    private ActiveTimeProbe _requestTimeProbe;
    private ActiveProbe _connectionProbe;

    private HmuxChannel[] _idle = new HmuxChannel[64];
    private volatile int _idleHead;
    private volatile int _idleTail;
    private int _idleSize = 16;

    private int _maxConnections = int.MaxValue / 2;

    private int _loadBalanceIdleTime = 10000;
    private volatile int _keepaliveCountTotal;

    private volatile int _activeCount;
    private volatile int _startingCount;

    private String _address;
    private int _port;

    public HmuxChannelFactory(String address, int port)
    {
      _address = address;
      _port = port;

      _log = Logger.GetLogger();

      _idleProbe = new ActiveProbe();
      _requestTimeProbe = new ActiveTimeProbe();
      _connectionProbe = new ActiveProbe();
    }

    public HmuxChannel OpenServer(String sessionId, HmuxChannelFactory xChannelFactory)
    {
      HmuxChannel channel = OpenRecycle();

      if (channel != null)
        return channel;

      Socket hmuxSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
      hmuxSocket.Connect(_address, _port);
      channel = new HmuxChannel(hmuxSocket, this);

      return channel;
    }

    private HmuxChannel OpenRecycle()
    {
      long now = DateTime.Now.Ticks;
      HmuxChannel channel = null;

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

      return null;
    }


  private ClientSocket Connect()
  {
    lock (this) {
      if (_maxConnections <= _activeCount + _startingCount)
        return null;

      _startingCount++;
    }

    State state = _state;
    if (! state.isInit()) {
      String message = String.Format("'{0}' connection cannot be opened because the server pool has not been started.", this);
      InvalidOperationException e = new InvalidOperationException(message);

      _log.Warning(message);

      throw e;
    }

    try {
      ReadWritePair pair = openTCPPair();
      ReadStream rs = pair.getReadStream();
      rs.setAttribute("timeout", new Integer((int) _loadBalanceSocketTimeout));

      synchronized (this) {
        _activeCount++;
        _connectCountTotal++;
      }

      ClientSocket stream = new ClientSocket(this, _streamCount++,
                                               rs, pair.getWriteStream());

      if (log.isLoggable(Level.FINER))
        log.finer("connect " + stream);

      if (_firstSuccessTime <= 0) {
        if (_state.isStarting()) {
          if (_loadBalanceWarmupTime > 0)
            _state = State.WARMUP;
          else
            _state = State.ACTIVE;

          _firstSuccessTime = Alarm.getCurrentTime();
        }

        if (_warmupState < 0)
          _warmupState = 0;
      }

      return stream;
    } catch (IOException e) {
      if (_log.IsLoggable())
        _log.log(Level.FINEST, this + " " + e.toString(), e);
      else
        log.finer(this + " " + e.toString());

      failConnect();

      return null;
    } finally {
      synchronized (this) {
        _startingCount--;
      }
    }
  }

    internal void Busy()
    {
      Trace.TraceInformation("HmuxChannelFactory.Busy() NYI");
    }

    internal void FailSocket()
    {
      Trace.TraceInformation("HmuxChannelFactory.FailSocket() NYI");
    }

    internal void SetCpuLoadAvg(double loadAvg)
    {
      Trace.TraceInformation("HmuxChannelFactory.SetCpuLoadAvg() NYI");
    }

    internal void Free(HmuxChannel hmuxChannel)
    {
      Trace.TraceInformation("HmuxChannelFactory.free() NYI");
    }

    internal void Close(HmuxChannel hmuxChannel)
    {
      Trace.TraceInformation("HmuxChannelFactory.Close(HmuxChannel) NYI");
    }

    internal void Close()
    {
      Trace.TraceInformation("HmuxChannelFactory.Close() NYI");
    }

    internal ActiveProbe GetConnectionProbe()
    {
      return _connectionProbe;
    }

    internal ActiveTimeProbe GetRequestTimeProbe()
    {
      return _requestTimeProbe;
    }

    internal ActiveProbe GetIdleProbe()
    {
      return _idleProbe;
    }

    internal string GetDebugId()
    {
      return _address + _port.ToString();
    }
  }

  enum State { 
  }
}