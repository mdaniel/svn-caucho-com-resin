using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Net.Sockets;
using System.Web;
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

    public HmuxChannel(Socket socket)
    {
      _socket = socket;
      _hmuxOutBuffer = new TempBuffer(1024);
      _hmuxInBuffer = new TempBuffer(1024);
    }

    public int Read(byte[] buffer)
    {
      return _socket.Receive(buffer);
    }

    public void StartChannel()
    {
      byte[] bytes = new byte[] { (byte)HMUX_CHANNEL, 0, 1 };
      Write(bytes);
    }

    public void WriteUrl(String path)
    {
      byte[] bytes = System.Text.Encoding.ASCII.GetBytes(path.ToCharArray());
      Write((byte)HMUX_URI);
      WriteRawString(bytes);
    }

    public void WriteRawString(byte[] bytes)
    {
      WriteHmuxLength(bytes.Length);
      Write(bytes);
    }

    public void WriteMethod(String method)
    {
      byte[] bytes = System.Text.Encoding.ASCII.GetBytes(method.ToCharArray());
      Write((byte)HMUX_METHOD);
      WriteRawString(bytes);
    }

    public void WriteBody(HttpRequest request)
    {
      int contentLength = request.ContentLength;
      if (contentLength == 0)
        return;

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
      Write((byte)HMUX_QUIT);
    }

    public void WriteExit()
    {
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
      Debug("SendHmux: [" + offset + "] [" + length + "]");
      return _socket.Send(buffer, offset, length, SocketFlags.None);
    }

    private int FillInBuffer()
    {
      _hmuxInBuffer.Reset();
      Debug("Filling In Buffer: ");
      int length = _socket.Receive(_hmuxInBuffer.Bytes);
      Debug("Filled In Buffer: " + length);
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
      Debug("read " + length + " bytes starting at " + offset);
      int l = length;
      int bytesRead = 0;
      while (l > 0) {
        int len = l > _hmuxInBuffer.Length - _hmuxInBuffer.Offset ? _hmuxInBuffer.Length - _hmuxInBuffer.Offset : l;
        Debug("  offset:" + offset + ", bytesToSend:" + length + ", len:" + len + ", l:" + l + ", hmuxInBuffer.Length:" + _hmuxInBuffer.Length + ", hmuxInBuffer.Offset:" + _hmuxInBuffer.Offset + "");
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
      Debug("read string of bytesToSend: " + length);
      byte[] buffer = new byte[length];
      Read(buffer, 0, length);
      String result = Encoding.ASCII.GetString(buffer);
      Debug("  string [" + result + "]");
      return result;
    }

    private int SkipBytes(int skipLength)
    {
      Debug("skip " + skipLength + " bytes");
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
      int bytesToSend = ReadHmuxLength();

      Debug("D:" + bytesToSend);

      int bytesSent = 0;
      while (bytesSent < bytesToSend) {
        int len = (bytesToSend - bytesSent) > _hmuxInBuffer.Length - _hmuxInBuffer.Offset ? _hmuxInBuffer.Length - _hmuxInBuffer.Offset : (bytesToSend - bytesSent);

        stream.Write(_hmuxInBuffer.Bytes, _hmuxInBuffer.Offset, len);

        _hmuxInBuffer.Offset = _hmuxInBuffer.Offset + len;
        bytesSent = bytesSent + len;

        if (bytesSent < bytesToSend && FillInBuffer() == -1)
          break;
      }

      return bytesSent;
    }

    private void AddHeader(HttpResponse response, String name, String value)
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
            Debug("setting charset: " + charset);
            response.Charset = charset;
          }

        }
      }
    }

    public void DoResponse(HttpContext context)
    {
      HttpResponse response = context.Response;
      Debug("Do Response");
      int code;
      while ((code = Read()) != -1) {
        Debug("Do Response: " + code);
        switch (code) {
          case HMUX_ACK: {
              Debug("read ack");
              int len = ReadHmuxLength();
              SkipBytes(len);

              break;
            }
          case HMUX_STATUS: {
              Debug("read status");
              response.Status = ReadString();

              break;
            }
          case HMUX_META_HEADER: {
              Debug("read meta header");
              SkipBytes(ReadHmuxLength());
              Read();
              SkipBytes(ReadHmuxLength());

              break;
            }
          case HMUX_HEADER: {
              Debug("Read header");
              String name = ReadString();
              Read();//HMUX_STRING
              String value = ReadString();
              AddHeader(response, name, value);

              break;
            }
          case HMUX_DATA: {
              RelayResponseData(response.OutputStream);

              break;
            }
          case HMUX_YIELD: {
              break;
            }
          case HMUX_EXIT: {
              return;
            }
          case HMUX_QUIT: {
              return;
            }
          default: { break; }
        }
      }
    }

    private void Debug(String str)
    {
      StreamWriter w = new StreamWriter(new FileStream("c:\\temp\\plugin.log", FileMode.Append));
      w.WriteLine(str);
      w.Flush();
      w.Close();
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
