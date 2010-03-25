using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;

namespace Caucho.IIS
{
  class HmuxChannel
  {
    public static int HMUX_CHANNEL = 'C';
    public static int HMUX_ACK = 'A';
    public static int HMUX_ERROR = 'E';
    public static int HMUX_YIELD = 'Y';
    public static int HMUX_QUIT = 'Q';
    public static int HMUX_EXIT = 'X';

    public static int HMUX_DATA = 'D';
    public static int HMUX_URI = 'U';
    public static int HMUX_STRING = 'S';
    public static int HMUX_HEADER = 'H';
    public static int HMUX_BINARY = 'B';
    public static int HMUX_PROTOCOL = 'P';
    public static int HMUX_META_HEADER = 'M';

    // The following are HTTP codes
    public static int CSE_NULL = '?';
    public static int CSE_PATH_INFO = 'b';
    public static int CSE_PROTOCOL = 'c';
    public static int CSE_REMOTE_USER = 'd';
    public static int CSE_QUERY_STRING = 'e';
    public static int HMUX_FLUSH = 'f';
    public static int CSE_SERVER_PORT = 'g';
    public static int CSE_REMOTE_HOST = 'h';
    public static int CSE_REMOTE_ADDR = 'i';
    public static int CSE_REMOTE_PORT = 'j';
    public static int CSE_REAL_PATH = 'k';
    public static int CSE_SCRIPT_FILENAME = 'l';
    public static int HMUX_METHOD = 'm';
    public static int CSE_AUTH_TYPE = 'n';
    public static int CSE_URI = 'o';
    public static int CSE_CONTENT_LENGTH = 'p';
    public static int CSE_CONTENT_TYPE = 'q';
    public static int CSE_IS_SECURE = 'r';
    public static int HMUX_STATUS = 's';
    public static int CSE_CLIENT_CERT = 't';
    public static int CSE_SERVER_TYPE = 'u';
    public static int HMUX_SERVER_NAME = 'v';

    public static int CSE_SEND_HEADER = 'G';

    private Socket _socket;
    private TempBuffer _buffer;

    public HmuxChannel(Socket socket)
    {
      _socket = socket;
      _buffer = new TempBuffer(8888);
    }

    public int Read(byte[] buffer)
    {
      return _socket.Receive(buffer);
    }

    public void StartChannel()
    {
      byte[] bytes = new byte[] { (byte)HMUX_CHANNEL, 0, 1 };
      _buffer.Write(bytes);
    }

    public void WriteUrl(String path)
    {
      _buffer.Write((byte)HMUX_URI);
      byte[] bytes = System.Text.Encoding.ASCII.GetBytes(path.ToCharArray());
      WriteRawString(bytes);
    }

    public void WriteRawString(byte[] bytes)
    {
      int len = bytes.Length;
      _buffer.Write((byte)(len >> 8));
      _buffer.Write((byte)len);
      _buffer.Write(bytes);
    }

    public void WriteMethod(String method)
    {
      _buffer.Write((byte)HMUX_METHOD);
      byte[] bytes = System.Text.Encoding.ASCII.GetBytes(method.ToCharArray());
      WriteRawString(bytes);
    }

    public void WriteQuit()
    {
      _buffer.Write((byte)HMUX_QUIT);
    }

    public void WriteExit()
    {
      _buffer.Write((byte)HMUX_EXIT);
    }

    public void Flush()
    {
      _socket.Send(_buffer.Bytes, 0, _buffer.Lenght, SocketFlags.None);
    }
  }

  /**
   * pooled buffer
   */
  class TempBuffer
  {
    public int Lenght { get { return _length; } }
    public byte[] Bytes { get { return _bytes; } }

    private int _length;
    private byte[] _bytes;

    public TempBuffer(int capacity)
    {
      _bytes = new byte[capacity];
      _length = 0;
    }

    public void Write(byte b)
    {
      if (_length == _bytes.Length) {
        byte[] temp = _bytes;
        _bytes = new byte[2 * temp.Length];
        Array.Copy(temp, 0, _bytes, 0, _length);
      }
      _bytes[_length] = b;
      _length++;
    }

    public void Write(byte[] buffer)
    {
      Write(buffer, 0, buffer.Length);
    }

    public void Write(byte[] buffer, int index, int length)
    {
      if (_length + length > _bytes.Length) {
        byte[] temp = _bytes;
        _bytes = new byte[_bytes.Length + length * 2];
        Array.Copy(temp, 0, _bytes, 0, _length);
      }

      Array.Copy(buffer, index, _bytes, _length, length);
      _length = _length + length;
    }
  }
}
