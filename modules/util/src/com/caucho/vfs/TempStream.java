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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.*;

public class TempStream extends StreamImpl
{
  private String _encoding;
  private TempBuffer _head;
  private TempBuffer _tail;
  private Path _backingDir;
  private Path _backingFile;
  private TempFile _tempBackingFile;
  private WriteStream _backingStream;
  private boolean _useBackingFile;
  private TempReadStream _tempReadStream;

  public TempStream(Path backingDir)
  {
    _backingDir = backingDir;
  }

  public TempStream()
  {
  }

  /**
   * Initializes the temp stream for writing.
   */
  public void openWrite()
  {
    TempBuffer ptr = _head;

    _head = null;
    _tail = null;

    _encoding = null;

    TempBuffer.freeAll(ptr);

    _useBackingFile = false;

    if (_backingStream != null) {
      try {
	_backingStream.close();
      } catch (IOException e) {
      }
      _backingStream = null;
    }
  }

  public byte []getTail()
  {
    return _tail.getBuffer();
  }

  public void changeToBackingFile(int index)
    throws IOException
  {
    if (_backingFile == null) {
      _backingFile = _backingDir.createTempFile("tmp", ".tmp");
      _tempBackingFile = new TempFile(_backingFile);
    }
    
    _backingStream = _backingFile.openWrite();
    _useBackingFile = true;

    TempBuffer next;
    for (; _head != null; _head = next) {
      next = _head._next;

      _backingStream.write(_head._buf, 0, _head._length);
      TempBuffer.free(_head);
    }
  }

  /**
   * Sets the encoding.
   */
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  /**
   * Gets the encoding.
   */
  public String getEncoding()
  {
    return _encoding;
  }

  public boolean canWrite() { return true; }
	   
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (_backingStream != null) {
      _backingStream.write(buf, offset, length);
    }
    else {
      int index = 0;
      while (index < length) {
	if (_tail == null)
	  addBuffer(TempBuffer.allocate());
	else if (_tail._length >= _tail._buf.length) {
	  if (_head._bufferCount < 8 || _backingDir == null)
	    addBuffer(TempBuffer.allocate());
	  else {
	    changeToBackingFile(index);
	    _backingStream.write(buf, offset, length);
	    return;
	  }
	}

	int sublen = _tail._buf.length - _tail._length;
	if (length - index < sublen)
	  sublen = length - index;

	System.arraycopy(buf, index + offset, _tail._buf, _tail._length, sublen);

	index += sublen;
	_tail._length += sublen;
      }
    }
  }

  private void addBuffer(TempBuffer buf)
  {
    buf._next = null;
    if (_tail != null) {
      _tail._next = buf;
      _tail = buf;
    } else {
      _tail = buf;
      _head = buf;
    }

    _head._bufferCount++;
  }

  public void flush()
    throws IOException
  {
    if (_backingStream != null)
      _backingStream.flush();
  }

  public void close()
    throws IOException
  {
    if (_backingStream != null) {
      _backingStream.close();
      _backingStream = null;
    }

    super.close();
  }

  /**
   * Opens a read stream to the buffer.
   */
  public ReadStream openRead()
    throws IOException
  {
    return openRead(false);
  }

  /**
   * Opens a read stream to the buffer.
   *
   * @param free if true, frees the buffer as it's read
   */
  public ReadStream openRead(boolean free)
    throws IOException
  {
    close();

    if (_useBackingFile)
      return _backingFile.openRead();
    else {
      TempReadStream read = new TempReadStream(_head);
      read.setFreeWhenDone(free);
      if (free) {
        _head = null;
        _tail = null;
      }
      read.setPath(getPath());
      return new ReadStream(read);
    }
  }

  /**
   * Opens a read stream to the buffer.
   *
   * @param free if true, frees the buffer as it's read
   */
  public void openRead(ReadStream rs, boolean free)
    throws IOException
  {
    close();

    if (_useBackingFile) {
      StreamImpl impl = _backingFile.openReadImpl();

      rs.init(impl, null);
    }
    else {
      if (_tempReadStream == null) {
	_tempReadStream = new TempReadStream();
	_tempReadStream.setPath(getPath());
      }

      _tempReadStream.init(_head);

      _tempReadStream.setFreeWhenDone(free);
      if (free) {
        _head = null;
        _tail = null;
      }

      rs.init(_tempReadStream, null);
    }
  }

  /**
   * Returns the head buffer.
   */
  public TempBuffer getHead()
  {
    return _head;
  }

  public void writeToStream(OutputStream os)
    throws IOException
  {
    for (TempBuffer ptr = _head; ptr != null; ptr = ptr._next) {
      os.write(ptr.getBuffer(), 0, ptr.getLength());
    }
  }

  /**
   * Returns the total length of the buffer's bytes
   */
  public int getLength()
  {
    int length = 0;
    
    for (TempBuffer ptr = _head; ptr != null; ptr = ptr._next) {
      length += ptr.getLength();
    }

    return length;
  }

  public ReadStream openRead(ReadStream s)
    throws IOException
  {
    close();

    if (_useBackingFile)
      return _backingFile.openRead();
    else {
      TempReadStream read = new TempReadStream(_head);
      read.setFreeWhenDone(false);
      read.setPath(getPath());
      s.init(read, null);
      return s;
    }
  }

  public void clearWrite()
  {
    TempBuffer ptr = _head;

    _head = null;
    _tail = null;

    TempBuffer.freeAll(ptr);

    if (_backingStream != null) {
      try {
	_backingStream.close();
	_backingStream = _backingFile.openWrite();
      } catch (Exception e) {
      }
    }
    _useBackingFile = false;
  }

  public void discard()
  {
    _head = null;
    _tail = null;

    if (_backingStream != null) {
      try {
	_backingStream.close();
	_backingStream = _backingFile.openWrite();
      } catch (Exception e) {
      }
    }
    _useBackingFile = false;
  }

  /**
   * Copies the temp stream;
   */
  public TempStream copy()
  {
    TempStream newStream = new TempStream();

    TempBuffer ptr = _head;

    for (; ptr != null; ptr = ptr.getNext()) {
      TempBuffer newPtr = TempBuffer.allocate();
      
      if (newStream._tail != null)
	newStream._tail.setNext(newPtr);
      else
	newStream._head = newPtr;
      newStream._tail = newPtr;

      newPtr.write(ptr.getBuffer(), 0, ptr.getLength());
    }

    return newStream;
  }

  /**
   * Clean up the temp stream.
   */
  public void destroy()
  {
    try {
      close();
    } catch (IOException e) {
    }

    try {
      TempFile tempBackingFile = _tempBackingFile;
      _tempBackingFile = tempBackingFile;
      
      if (tempBackingFile != null)
	tempBackingFile.remove();
    } catch (Throwable e) {
    }

    TempBuffer ptr = _head;
    
    _head = null;
    _tail = null;

    TempBuffer.freeAll(ptr);
  }
}
