/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.caucho.vfs.TempBuffer;

import com.caucho.quercus.QuercusModuleException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.QuercusClass;

/**
 * A stream that has its operations mediated by a Quercus object.
 */
public class WrappedStream implements BinaryInput, BinaryOutput {
  private byte []printBuffer = new byte[1];

  private Env _env;
  private Value _wrapper;
  private InputStream _is;
  private OutputStream _os;
  private int _buffer;
  private boolean _doUnread = false;

  private int _writeLength;

  private WrappedStream(Env env, Value wrapper)
  {
    _env = env;

    _wrapper = wrapper;
  }

  public WrappedStream(Env env, QuercusClass qClass, 
                       StringValue path, StringValue mode, LongValue options)
  {
    _env = env;

    _wrapper = qClass.callNew(_env, new Value[0]);

    _wrapper.callMethod(_env, "stream_open", 
                        path, mode, options, NullValue.NULL);
  }

  public InputStream getInputStream()
  {
    if (_is == null)
      _is = new WrappedInputStream();

    return _is;
  }

  public OutputStream getOutputStream()
  {
    if (_os == null)
      _os = new WrappedOutputStream();

    return _os;
  }

  /**
   * Opens a new copy.
   */
  public BinaryInput openCopy()
    throws IOException
  {
    return new WrappedStream(_env, _wrapper);
  }

  /**
   * Sets the current read encoding.  The encoding can either be a
   * Java encoding name or a mime encoding.
   *
   * @param encoding name of the read encoding
   */
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
  }

  public void closeRead()
  {
    close();
  }

  public void closeWrite()
  {
    close();
  }

  public void close()
  {
    _wrapper.callMethod(_env, "stream_close");
  }

  /**
   * Reads a character from a file, returning -1 on EOF.
   */
  public int read()
    throws IOException
  {
    if (_doUnread) {
      _doUnread = false;

      return _buffer;
    } else {
      Value output = _wrapper.callMethod(_env, "stream_read", LongValue.ONE);

      _buffer = (int) output.toLong();

      return _buffer;
    }
  }

  /**
   * Unread a character.
   */
  public void unread()
    throws IOException
  {
    _doUnread = true;
  }

  public int read(byte []buffer, int offset, int length)
  {
    Value output = _wrapper.callMethod(_env, "stream_read", 
                                       LongValue.create(length));

    // XXX "0"?
    if (! output.toBoolean())
      return -1;

    byte []outputBytes = output.toString().getBytes();

    if (length > outputBytes.length)
      length = outputBytes.length;

    System.arraycopy(outputBytes, 0, buffer, offset, length);

    return length;
  }

  /**
   * Reads a Binary string.
   */
  public BinaryValue read(int length)
    throws IOException
  {
    Value output = _wrapper.callMethod(_env, "stream_read", 
                                       LongValue.create(length));

    return output.toBinaryValue(_env);
  }

  /**
   * Reads the optional linefeed character from a \r\n
   */
  public boolean readOptionalLinefeed()
    throws IOException
  {
    int ch = read();

    if (ch == '\n') {
      return true;
    }
    else {
      unread();
      return false;
    }
  }

    /**
   * Reads a line from a file, returning null on EOF.
   */
  public StringValue readLine(int length)
    throws IOException
  {
    StringBuilderValue sb = new StringBuilderValue();

    int ch;

    for (; length > 0 && (ch = read()) >= 0; length--) {
      if (ch == '\n') {
        sb.append((char) ch);
        return sb;
      }
      else if (ch == '\r') {
        sb.append('\r');

        int ch2 = read();

        if (ch == '\n')
          sb.append('\n');
        else
          unread();

        return sb;
      }
      else
        sb.append((char) ch);
    }

    if (sb.length() == 0)
      return null;
    else
      return sb;
  }
  
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    BinaryBuilderValue bb = new BinaryBuilderValue(buffer, offset, length);

    Value output = _wrapper.callMethod(_env, "stream_write", bb);

    _writeLength = (int) output.toLong();
  }

  /**
   * Writes to a stream.
   */
  public int write(InputStream is, int length)
  {
    int writeLength = 0;

    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.getBuffer();

    try {
      while (length > 0) {
        int sublen;

        if (length < buffer.length)
          sublen = length;
        else
          sublen = buffer.length;

        sublen = is.read(buffer, 0, sublen);

        if (sublen < 0)
          break;

        for (int offset = 0; offset < sublen;) {
          write(buffer, offset, sublen);

          if (_writeLength > 0)
            offset += _writeLength;
          else
            return writeLength;
        }

        writeLength += sublen;
        length -= sublen;
      }

      return writeLength;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      TempBuffer.free(tb);
    }
  }

  /**
   * Prints a string to a file.
   */
  public void print(char v)
    throws IOException
  {
    printBuffer[0] = (byte) v;

    write(printBuffer, 0, 1);
  }

  /**
   * Prints a string to a file.
   */
  public void print(String v)
    throws IOException
  {
    for (int i = 0; i < v.length(); i++)
      print(v.charAt(i));
  }

  /**
   * Returns true if end-of-file has been reached
   */
  public boolean isEOF()
  {
    return _wrapper.callMethod(_env, "stream_eof").toBoolean();
  }

  /**
   * Tells the position in the stream
   */
  public long getPosition()
  {
    return _wrapper.callMethod(_env, "stream_tell").toLong();
  }

  /**
   * Sets the position.
   */
  public boolean setPosition(long offset)
  {
    LongValue offsetValue = LongValue.create(offset);
    LongValue whenceValue = LongValue.create(SEEK_SET);

    return _wrapper.callMethod(_env, "stream_seek", 
                               offsetValue, whenceValue).toBoolean();
  }

  public long seek(long offset, int whence)
  {
    LongValue offsetValue = LongValue.create(offset);
    LongValue whenceValue = LongValue.create(whence);

    return _wrapper.callMethod(_env, "stream_seek", 
                               offsetValue, whenceValue).toLong();
  }

  public void flush()
    throws IOException
  {
    if (! _wrapper.callMethod(_env, "stream_flush").toBoolean())
      throw new IOException(); // Get around java.io.Flushable
  }

  public Value stat()
  {
    return _wrapper.callMethod(_env, "stream_flush");
  }

  private class WrappedInputStream extends InputStream {
    public int read()
      throws IOException
    {
      return WrappedStream.this.read();
    }
  }

  private class WrappedOutputStream extends OutputStream {
    public void write(int b)
      throws IOException
    {
      _wrapper.callMethod(_env, "stream_write", LongValue.create(b));
    }
  }
}
