/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.jms.message;

import java.util.logging.Level;

import java.io.IOException;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageFormatException;

import com.caucho.util.CharBuffer;

import com.caucho.vfs.TempStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import com.caucho.jms.JMSExceptionWrapper;

/**
 * A byte-stream message.
 */
public class BytesMessageImpl extends MessageImpl implements BytesMessage  {
  private TempStream _tempStream;
  private ReadStream _rs;
  private WriteStream _ws;

  /**
   * Sets the body for reading.
   */
  public void setReceive()
    throws JMSException
  {
    super.setReceive();
    
    reset();
  }

  /**
   * Set the stream for reading.
   */
  public void reset()
    throws JMSException
  {
    setBodyReadOnly();
    
    try {
      // XXX: test for null
      if (_ws != null)
	_ws.close();

      if (_tempStream != null) {
	if (_rs != null)
	  _rs.close();
	
	_rs = _tempStream.openRead();
      }
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read a boolean from the stream.
   */
  public boolean readBoolean()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      return is.read() == 1;
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read a byte from the stream.
   */
  public byte readByte()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      return (byte) is.read();
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read an unsigned byte from the stream.
   */
  public int readUnsignedByte()
    throws JMSException
  {
    ReadStream is = getReadStream();

    if (is == null)
      return -1;

    try {
      return is.read();
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read a short from the stream.
   */
  public short readShort()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();    
      int d2 = is.read();    
    
      return (short) ((d1 << 8) + d2);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read an unsigned short from the stream.
   */
  public int readUnsignedShort()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();    
      int d2 = is.read();    
    
      return ((d1 << 8) + d2);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read an integer from the stream.
   */
  public int readInt()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();    
      int d2 = is.read();    
      int d3 = is.read();    
      int d4 = is.read();    
    
      return (d1 << 24) + (d2 << 16) + (d3 << 8) + d4;
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read a long from the stream.
   */
  public long readLong()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      long d1 = is.read();    
      long d2 = is.read();    
      long d3 = is.read();    
      long d4 = is.read();    
      long d5 = is.read();    
      long d6 = is.read();    
      long d7 = is.read();    
      long d8 = is.read();    
    
      return ((d1 << 56) +
              (d2 << 48) +
              (d3 << 40) +
              (d4 << 32) +
              (d5 << 24) +
              (d6 << 16) +
              (d7 << 8) +
              (d8));
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read a float from the stream.
   */
  public float readFloat()
    throws JMSException
  {
    return Float.intBitsToFloat(readInt());
  }

  /**
   * Read a double from the stream.
   */
  public double readDouble()
    throws JMSException
  {
    return Double.longBitsToDouble(readLong());
  }

  /**
   * Read a character object from the stream.
   */
  public char readChar()
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      int d1 = is.read();    
      int d2 = is.read();

      return (char) ((d1 << 8) + d2);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read a string from the stream.
   */
  public String readUTF()
    throws JMSException
  {
    ReadStream is = getReadStream();
    CharBuffer cb = new CharBuffer();

    try {
      int d1;
      
      while ((d1 = is.read()) > 0) {
        if (d1 < 0x80)
          cb.append((char) d1);
        else if ((d1 & 0xe0) == 0xc0) {
          int d2 = is.read();

          cb.append((char) (((d1 & 0x1f) << 6) + (d2 & 0x3f)));
        }
        else if ((d1 & 0xf0) == 0xe0) {
          int d2 = is.read();
          int d3 = is.read();

          cb.append((char) (((d1 & 0xf) << 12) +
                            ((d2 & 0x3f) << 6) +
                            (d3 & 0x3f)));
        }
	else
	  throw new MessageFormatException(L.l("invalid UTF-8 in bytes message"));
      }

      if (d1 < 0)
	throw new MessageEOFException("end of message in byte stream");
    } catch (JMSException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new JMSExceptionWrapper(e);
    }

    return cb.toString();
  }

  /**
   * Read a byte array object from the stream.
   */
  public int readBytes(byte []value)
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      return is.read(value);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Read a byte array object from the stream.
   */
  public int readBytes(byte []value, int length)
    throws JMSException
  {
    ReadStream is = getReadStream();

    try {
      return is.read(value, 0, length);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  protected ReadStream getReadStream()
    throws JMSException
  {
    checkBodyReadable();

    /* ejb/6a87
    if (_rs == null)
      throw new MessageEOFException(L.l("bytes message may not be read"));
    */
      
    return _rs;
  }

  /**
   * Clears the message and puts it into write mode.
   */
  public void clearBody()
    throws JMSException
  {
    super.clearBody();
    
    _ws = null;
    _tempStream = null;
    _rs = null;
  }

  /**
   * Writes a boolean to the stream.
   */
  public void writeBoolean(boolean b)
    throws JMSException
  {
    try {
      getWriteStream().write(b ? 1 : 0);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes a byte to the stream.
   */
  public void writeByte(byte b)
    throws JMSException
  {
    try {
      getWriteStream().write(b);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes a short to the stream.
   */
  public void writeShort(short s)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();
    
      ws.write(s >> 8);
      ws.write(s);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes an integer to the stream.
   */
  public void writeInt(int i)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();
    
      ws.write(i >> 24);
      ws.write(i >> 16);
      ws.write(i >> 8);
      ws.write(i);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes a long to the stream.
   */
  public void writeLong(long l)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();
    
      ws.write((int) (l >> 56));
      ws.write((int) (l >> 48));
      ws.write((int) (l >> 40));
      ws.write((int) (l >> 32));
      ws.write((int) (l >> 24));
      ws.write((int) (l >> 16));
      ws.write((int) (l >> 8));
      ws.write((int) l);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes a float to the stream.
   */
  public void writeFloat(float f)
    throws JMSException
  {
    int i = Float.floatToIntBits(f);
    writeInt(i);
  }

  /**
   * Writes a double to the stream.
   */
  public void writeDouble(double d)
    throws JMSException
  {
    long l = Double.doubleToLongBits(d);
    writeLong(l);
  }

  /**
   * Writes a string to the stream.
   */
  public void writeUTF(String s)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();

      int len = s.length();
      for (int i = 0; i < len; i++) {
        int ch = s.charAt(i);

        if (ch == 0) {
          ws.write(0xc0);
          ws.write(0x80);
        }
        else if (ch < 0x80)
          ws.write(ch);
        else if (ch < 0x800) {
          ws.write(0xc0 + ((ch >> 6) & 0x1f));
          ws.write(0x80 + (ch & 0x3f));
        }
        else if (ch < 0x8000) {
          ws.write(0xe0 + ((ch >> 12) & 0x0f));
          ws.write(0x80 + ((ch >> 6) & 0x3f));
          ws.write(0x80 + (ch & 0x3f));
        }
      }
      
      ws.write(0);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes a character to the stream.
   */
  public void writeChar(char ch)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();

      ws.write(ch >> 8);
      ws.write(ch);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes a byte array to the stream.
   */
  public void writeBytes(byte []buf)
    throws JMSException
  {
    writeBytes(buf, 0, buf.length);
  }

  /**
   * Writes a byte array to the stream.
   */
  public void writeBytes(byte []buf, int offset, int length)
    throws JMSException
  {
    try {
      WriteStream ws = getWriteStream();

      ws.write(buf, offset, length);
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  /**
   * Writes the next object.
   */
  public void writeObject(Object obj)
    throws JMSException
  {
    if (obj instanceof Boolean)
      writeBoolean(((Boolean) obj).booleanValue());
    else if (obj instanceof Byte)
      writeByte(((Byte) obj).byteValue());
    else if (obj instanceof Short)
      writeShort(((Short) obj).shortValue());
    else if (obj instanceof Character)
      writeChar(((Character) obj).charValue());
    else if (obj instanceof Integer)
      writeInt(((Integer) obj).intValue());
    else if (obj instanceof Long)
      writeLong(((Long) obj).longValue());
    else if (obj instanceof Float)
      writeFloat(((Float) obj).floatValue());
    else if (obj instanceof Double)
      writeDouble(((Double) obj).doubleValue());
    else if (obj instanceof String)
      writeUTF((String) obj);
    else if (obj instanceof byte[])
      writeBytes((byte[]) obj);
    else
      throw new JMSException("jms");
  }

  public long getBodyLength()
    throws JMSException
  {
    if (_tempStream == null)
      return 0;
    else
      return _tempStream.getLength();
  }

  protected WriteStream getWriteStream()
    throws JMSException
  {
    checkBodyWriteable();

    if (_tempStream == null)
      _tempStream = new TempStream(null);
    
    if (_ws == null)
      _ws = new WriteStream(_tempStream);

    return _ws;
  }

  public MessageImpl copy()
  {
    BytesMessageImpl msg = new BytesMessageImpl();

    copy(msg);

    return msg;
  }

  protected void copy(BytesMessageImpl newMsg)
  {
    super.copy(newMsg);

    try {
      if (_ws != null)
	_ws.flush();

      if (_tempStream != null)
	newMsg._tempStream = _tempStream.copy();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}

