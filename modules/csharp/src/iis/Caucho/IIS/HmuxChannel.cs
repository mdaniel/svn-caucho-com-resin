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
using System.Collections.Specialized;
using System.Linq;
using System.Text;
using System.IO;
using System.Net.Sockets;
using System.Web;
using System.Diagnostics;
/*
s\x00\x06200 OKM\x00\x08cpu-loadS\x00\x010H\x00\x0eContent-LengthS\x00\x0212H\x00\x0cContent-TypeS\x00\x18text/html; charset=utf-8G\x00\x00D\x00\x0cHello World
Q
 */

namespace Caucho.IIS
{
  public class HmuxChannel
  {
    public const int HMUX_CHANNEL = 'C';
    public const int HMUX_ACK = 'A';
    public const int HMUX_ERROR = 'E';
    public const int HMUX_YIELD = 'Y';
    public const int HMUX_QUIT = 'Q';
    public const int HMUX_EXIT = 'X';

    public const int HMUX_DATA = 'D';
    public const int HMUX_URI = 'U';
    public const int HMUX_STRING = 'S';
    public const int HMUX_HEADER = 'H';
    public const int HMUX_BINARY = 'B';
    public const int HMUX_PROTOCOL = 'P';
    public const int HMUX_META_HEADER = 'M';

    // The following are HTTP codes
    public const int CSE_NULL = '?';
    public const int CSE_PATH_INFO = 'b';
    public const int CSE_PROTOCOL = 'c';
    public const int CSE_REMOTE_USER = 'd';
    public const int CSE_QUERY_STRING = 'e';
    public const int HMUX_FLUSH = 'f';
    public const int CSE_SERVER_PORT = 'g';
    public const int CSE_REMOTE_HOST = 'h';
    public const int CSE_REMOTE_ADDR = 'i';
    public const int CSE_REMOTE_PORT = 'j';
    public const int CSE_REAL_PATH = 'k';
    public const int CSE_SCRIPT_FILENAME = 'l';
    public const int HMUX_METHOD = 'm';
    public const int CSE_AUTH_TYPE = 'n';
    public const int CSE_URI = 'o';
    public const int CSE_CONTENT_LENGTH = 'p';
    public const int CSE_CONTENT_TYPE = 'q';
    public const int CSE_IS_SECURE = 'r';
    public const int HMUX_STATUS = 's';
    public const int CSE_CLIENT_CERT = 't';
    public const int CSE_SERVER_TYPE = 'u';
    public const int HMUX_SERVER_NAME = 'v';

    public const int CSE_SEND_HEADER = 'G';

    private Socket _socket;
    private BufferedStream _stream;

    private String _traceId;
    private HmuxChannelFactory _pool;

    public HmuxChannel(Socket socket, HmuxChannelFactory pool)
    {
      _socket = socket;
      _stream = new BufferedStream(new NetworkStream(_socket));
      _pool = pool;
      _traceId = _socket.Handle.ToInt32().ToString();
    }

    public HmuxChannelFactory GetPool()
    {
      return _pool;
    }

    public BufferedStream GetSocketStream()
    {
      return _stream;
    }

    public String GetTraceId()
    {
      return _traceId;
    }

    public void Free(long requestStartTime)
    {
      Trace.TraceInformation("HmuxChannel.Free() NYI");
    }

    public void Close()
    {
      Trace.TraceInformation("HmuxChannel.Close() NYI");
    }

    public void SetIdleStartTime(long p)
    {
      Trace.TraceInformation("HmuxChannel.SetIdleStartTime() NYI");
    }
  }
}
