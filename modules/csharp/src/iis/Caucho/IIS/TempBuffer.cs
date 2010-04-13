using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Diagnostics;

namespace Caucho.IIS
{
  public class TempBuffer
  {
    public int Length { get { return _length; } set { _length = value; } }
    public int Capacity { get { return _bytes.Length; } }
    public int Offset { get { return _offset; } set { _offset = value; } }
    public byte[] Bytes { get { return _bytes; } }

    private int _offset;
    private int _length;
    private byte[] _bytes;

    public TempBuffer()
    {
      init(8192);
    }

    public TempBuffer(int capacity)
    {
      init(capacity);
    }

    private void init(int capacity)
    {
      _bytes = new byte[capacity];
      _offset = 0;
    }

    public void Reset()
    {
      _offset = 0;
      _length = 0;
    }

    public byte[] GetBuffer()
    {
      return _bytes;
    }

    public static TempBuffer Allocate()
    {
      return new TempBuffer();
    }

    internal static void Free(TempBuffer tempBuf)
    {
      Trace.TraceInformation("TempBuffer.Free() NYI");
    }
  }
}
