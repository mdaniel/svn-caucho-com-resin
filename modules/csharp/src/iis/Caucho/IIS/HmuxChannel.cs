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
  class HmuxChannel
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
    private TempBuffer _hmuxOutBuffer;
    private TempBuffer _hmuxInBuffer;
    private String _traceId;

    public HmuxChannel(Socket socket)
    {
      _socket = socket;
      _traceId = _socket.Handle.ToInt32().ToString();
      _hmuxOutBuffer = new TempBuffer(1024);
      _hmuxInBuffer = new TempBuffer(1024);
    }

    public int Read(byte[] buffer)
    {
      return _socket.Receive(buffer);
    }

    public void StartChannel()
    {
      Trace.TraceInformation("Hmux[{0}] start request", _traceId);
      byte[] bytes = new byte[] { (byte)HMUX_CHANNEL, 0, 1 };
      Write(bytes);
    }

    public void WriteUri(String uri)
    {
      String escaped = Uri.EscapeUriString(uri);
      Trace.TraceInformation("Hmux[{0}] U-r:uri {1}->{2}", _traceId, uri, escaped);
      WriteRequestString(HMUX_URI, escaped);
    }

    public void WriteRequestString(int code, String str)
    {
      Write((byte)code);
      if (str == null) {
        WriteHmuxLength(0);
      } else {
        byte[] bytes = System.Text.Encoding.ASCII.GetBytes(str.ToCharArray());
        WriteHmuxLength(bytes.Length);
        Write(bytes);
      }
    }

    public void WriteHttpMethod(String method)
    {
      Trace.TraceInformation("Hmux[{0}] m-r:method {1}", _traceId, method);
      WriteRequestString(HMUX_METHOD, method);
    }

    public void WriteServerVariables(NameValueCollection serverVariables)
    {
      String protocol = serverVariables.Get("HTTP_VERSION");
      Trace.TraceInformation("Hmux[{0}] c-r:protocol {1}", _traceId, protocol);
      WriteRequestString(CSE_PROTOCOL, protocol);

      String remoteAddr = serverVariables.Get("REMOTE_ADDR");
      Trace.TraceInformation("Hmux[{0}] i-r:remote address {1}", _traceId, remoteAddr);
      WriteRequestString(CSE_REMOTE_ADDR, remoteAddr);

      String remoteHost = serverVariables.Get("REMOTE_HOST");
      if (remoteHost == null)
        remoteHost = remoteAddr;

      Trace.TraceInformation("Hmux[{0}] h-r:remote host {1}", _traceId, remoteHost);
      WriteRequestString(CSE_REMOTE_HOST, remoteHost);

      String remotePort = serverVariables.Get("REMOTE_PORT");
      Trace.TraceInformation("Hmux[{0}] j-r:remote port {1}", _traceId, remotePort);
      WriteRequestString(CSE_REMOTE_PORT, remotePort);

      String serverName = serverVariables.Get("SERVER_NAME");
      Trace.TraceInformation("Hmux[{0}] v-r:server name {1}", _traceId, remotePort);
      WriteRequestString(HMUX_SERVER_NAME, serverName);

      String serverPort = serverVariables.Get("SERVER_PORT");
      Trace.TraceInformation("Hmux[{0}] g-r:server name {1}", _traceId, serverPort);
      WriteRequestString(CSE_SERVER_PORT, serverPort);

      Trace.TraceInformation("Hmux[{0}] u-r:server type {1}", _traceId, serverPort);
      WriteRequestString(CSE_SERVER_TYPE, "IIS");
    }

    public void RelayRequestBody(HttpRequest request)
    {
      int contentLength = request.ContentLength;
      if (contentLength == 0)
        return;

      Trace.TraceInformation("Hmux[{0}] D-r:({1})", _traceId, contentLength);
      Write((byte)HMUX_DATA);
      WriteHmuxLength(contentLength);

      if (_hmuxOutBuffer.Offset == _hmuxOutBuffer.Capacity) {
        FlushBuffer();
      }

      int len;
      while ((len
        = request.InputStream.Read(
        _hmuxOutBuffer.Bytes,
        _hmuxOutBuffer.Offset,
        _hmuxOutBuffer.Capacity - _hmuxOutBuffer.Offset)) > 0) {
        _hmuxOutBuffer.Offset = _hmuxOutBuffer.Offset + len;
        FlushBuffer();
      }
    }
    public void WriteQuit()
    {
      Trace.TraceInformation("Hmux[{0}] Q-r: end of request", _traceId);
      Write((byte)HMUX_QUIT);
    }

    public void WriteExit()
    {
      Trace.TraceInformation("Hmux[{0}] E-r: exit", _traceId);
      Write((byte)HMUX_EXIT);
    }

    private void WriteHmuxLength(int length)
    {
      Write((byte)((length >> 8) & 0xff));
      Write((byte)(length & 0xff));
    }

    private void Write(byte b)
    {
      if (_hmuxOutBuffer.Offset == _hmuxOutBuffer.Capacity)
        FlushBuffer();
      _hmuxOutBuffer.Bytes[_hmuxOutBuffer.Offset++] = b;
    }

    private void Write(byte[] bytes)
    {
      Write(bytes, 0, bytes.Length);
    }

    private void Write(byte[] bytes, int offset, int length)
    {
      if (length > _hmuxOutBuffer.Capacity) {
        FlushBuffer();
        SendHmux(bytes, offset, length);
      } else if (_hmuxOutBuffer.Offset + length < _hmuxOutBuffer.Capacity) {
        Array.Copy(bytes, 0, _hmuxOutBuffer.Bytes, _hmuxOutBuffer.Offset, length);
        _hmuxOutBuffer.Offset = _hmuxOutBuffer.Offset + length;
      } else {
        FlushBuffer();
        Array.Copy(bytes, offset, _hmuxOutBuffer.Bytes, 0, length);
        _hmuxOutBuffer.Offset = length;
      }
    }

    public void FlushBuffer()
    {
      SendHmux(_hmuxOutBuffer.Bytes, 0, _hmuxOutBuffer.Offset);
      _hmuxOutBuffer.Reset();
    }

    /**
     * Send data directly through the Hmux Socket
     */
    private int SendHmux(byte[] buffer, int offset, int length)
    {
      return _socket.Send(buffer, offset, length, SocketFlags.None);
    }

    private int FillInBuffer()
    {
      _hmuxInBuffer.Reset();
      int length = _socket.Receive(_hmuxInBuffer.Bytes);
      if (length == -1)
        return -1;

      _hmuxInBuffer.Length = length;

      return length;
    }

    public int ReadHmuxLength()
    {
      int l1 = Read() & 0xff;
      int l2 = Read() & 0xff;

      if (l2 < 0)
        return -1;

      return (l1 << 8) + (l2 & 0xff);
    }

    /**
     * Reads a byte from connected _hmuxInBuffer and advances _hmuxInBuffer.Offset
     */
    public int Read()
    {
      if (_hmuxInBuffer.Offset >= _hmuxInBuffer.Length) {
        if (FillInBuffer() == -1)
          return -1;
      }

      return _hmuxInBuffer.Bytes[_hmuxInBuffer.Offset++];
    }

    /**
     * Reads bytes from connected _hmuxInBuffer and advances _hmuxInBuffer.Offset
     */
    private int Read(byte[] buffer, int offset, int length)
    {
      int l = length;
      int bytesRead = 0;
      while (l > 0) {
        int len = l > _hmuxInBuffer.Length - _hmuxInBuffer.Offset ? _hmuxInBuffer.Length - _hmuxInBuffer.Offset : l;
        Array.Copy(_hmuxInBuffer.Bytes, _hmuxInBuffer.Offset, buffer, offset, len);
        _hmuxInBuffer.Offset = _hmuxInBuffer.Offset + len;
        offset = offset + len;

        bytesRead = bytesRead + len;

        if (len < l && FillInBuffer() == -1)
          return bytesRead;

        l = l - len;
      }

      return bytesRead;
    }

    private String ReadString()
    {
      int length = ReadHmuxLength();
      byte[] buffer = new byte[length];
      Read(buffer, 0, length);
      String result = Encoding.ASCII.GetString(buffer);
      return result;
    }

    private int SkipBytes(int skipLength)
    {
      int skipped = 0;
      int l = skipLength;
      while (l > 0) {
        int len = l > _hmuxInBuffer.Length - _hmuxInBuffer.Offset ? _hmuxInBuffer.Length - _hmuxInBuffer.Offset : l;
        _hmuxInBuffer.Offset = _hmuxInBuffer.Offset + len;
        skipped = skipped + len;

        if (len < l && FillInBuffer() == -1)
          return l;

        l = l - len;
      }

      return skipped;
    }

    public int RelayResponseData(Stream stream)
    {
      int bytesToRelay = ReadHmuxLength();
      Trace.TraceInformation("Hmux[{0}] D-w:({1})", _traceId, bytesToRelay);

      int bytesSent = 0;
      while (bytesSent < bytesToRelay) {
        int len = (bytesToRelay - bytesSent) > _hmuxInBuffer.Length - _hmuxInBuffer.Offset ? _hmuxInBuffer.Length - _hmuxInBuffer.Offset : (bytesToRelay - bytesSent);

        stream.Write(_hmuxInBuffer.Bytes, _hmuxInBuffer.Offset, len);

        _hmuxInBuffer.Offset = _hmuxInBuffer.Offset + len;
        bytesSent = bytesSent + len;

        if (bytesSent < bytesToRelay && FillInBuffer() == -1)
          break;
      }

      return bytesSent;
    }

    private void RelayResponseHeader(HttpResponse response, String name, String value)
    {
      response.Headers.Add(name, value);

      if ("Content-Type".Equals(name)) {
        int charsetIdx = value.IndexOf("charset");
        if (charsetIdx > -1) {
          String charset = null;
          int start = -1;
          int end = value.Length;
          for (int i = charsetIdx + 7; i < value.Length; i++) {
            char c = value[i];
            switch (c) {
              case '=': {
                  start = i;
                  break;
                }
              case ';':
              case ' ': {
                  end = i;
                  break;
                }
            }
          }
          if (start > -1 && end > start) {
            charset = value.Substring(start + 1, end - start - 1);
            response.Charset = charset;
          }
        }
      }
    }

    public void DoResponse(HttpContext context)
    {
      HttpResponse response = context.Response;
      int code;
      while ((code = Read()) != -1) {
        switch (code) {
          case HMUX_ACK: {
              Trace.TraceInformation("Hmux[{0}] A-w: Ack", _traceId);
              int len = ReadHmuxLength();
              SkipBytes(len);

              break;
            }
          case HMUX_STATUS: {
              response.Status = ReadString();
              Trace.TraceInformation("Hmux[{0}] s-w: {1}", _traceId, response.Status);

              break;
            }
          case HMUX_META_HEADER: {
              Trace.TraceInformation("Hmux[{0}] M-w", _traceId, response.Status);
              SkipBytes(ReadHmuxLength());
              Read();
              SkipBytes(ReadHmuxLength());

              break;
            }
          case HMUX_HEADER: {
              String name = ReadString();
              Read();//HMUX_STRING
              String value = ReadString();
              RelayResponseHeader(response, name, value);
              Trace.TraceInformation("Hmux[{0}] H-w: {1}={2}", _traceId, name, value);

              break;
            }
          case CSE_SEND_HEADER: {
              Trace.TraceInformation("Hmux[{0}] G-w", _traceId);
              break;
            }
          case HMUX_DATA: {
              RelayResponseData(response.OutputStream);

              break;
            }
          case HMUX_YIELD: {
              Trace.TraceInformation("Hmux[{0}] Y-w", _traceId);

              break;
            }
          case HMUX_EXIT: {
              Trace.TraceInformation("Hmux[{0}] E-w", _traceId);

              return;
            }
          case HMUX_QUIT: {
              Trace.TraceInformation("Hmux[{0}] Q-w", _traceId);

              return;
            }
          default: { break; }
        }
      }
    }
  }

  /**
   * pooled buffer
   */
  class TempBuffer
  {
    public int Length { get { return _length; } set { _length = value; } }
    public int Capacity { get { return _bytes.Length; } }
    public int Offset { get { return _offset; } set { _offset = value; } }
    public byte[] Bytes { get { return _bytes; } }

    private int _offset;
    private int _length;
    private byte[] _bytes;

    public TempBuffer(int capacity)
    {
      _bytes = new byte[capacity];
      _offset = 0;
    }

    public void Reset()
    {
      _offset = 0;
      _length = 0;
    }
  }
}
