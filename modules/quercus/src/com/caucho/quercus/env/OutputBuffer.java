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
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents a PHP output buffer
 */
public class OutputBuffer {
  public static final int PHP_OUTPUT_HANDLER_START = 0;
  public static final int PHP_OUTPUT_HANDLER_CONT = 1;
  public static final int PHP_OUTPUT_HANDLER_END = 2;

  private static final Logger log
    = Logger.getLogger(OutputBuffer.class.getName());

  private int _state;
  private boolean _haveFlushed;
  private Callback _callback;
  
  private final boolean _erase;
  private final int _chunkSize;
  private final int _level;

  private final OutputBuffer _next;

  private final TempStream _tempStream;
  private final WriteStream _out;

  private final Env _env;

  OutputBuffer(OutputBuffer next, Env env, Callback callback, 
               int chunkSize, boolean erase)
  {
    _next = next;

    if (_next != null)
      _level = _next._level + 1;
    else
      _level = 1;

    _erase = erase;
    _chunkSize = chunkSize;

    _env = env;
    _callback = callback;

    _tempStream = new TempStream();
    _out = new WriteStream(_tempStream);
    _state = 1 << PHP_OUTPUT_HANDLER_START;
    _haveFlushed = false;
  }

  /**
   * Returns the next output buffer;
   */
  public OutputBuffer getNext()
  {
    return _next;
  }

  /**
   * Returns the writer.
   */
  public WriteStream getOut()
  {
    return _out;
  }

  /**
   * Returns the buffer contents.
   */
  public Value getContents()
  {
    try {
      _out.flush();

      ReadStream rs = _tempStream.openRead(false);
      BinaryBuilderValue bb = new BinaryBuilderValue();
      int ch;

      // XXX: encoding
      while ((ch = rs.read()) >= 0) {
        bb.appendByte(ch);
      }

      return bb;
    } catch (IOException e) {
      _env.error(e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the buffer length.
   */
  public long getLength()
  {
    try {
      _out.flush();

      return (long)_tempStream.getLength();
    } catch (IOException e) {
      _env.error(e.toString(), e);

      return -1L;
    }
  }

  /**
   * Returns the nesting level.
   */
  public int getLevel()
  {
    return _level;
  }

  /**
   * Returns true if this buffer has ever been flushed.
   */
  public boolean haveFlushed()
  {
    return _haveFlushed;
  }

  /**
   * Returns the erase flag.
   */
  public boolean getEraseFlag()
  {
    // XXX: Why would anyone need this?  If the erase flag is false,
    // that supposedly means that the buffer will not be destroyed 
    // until the script finishes, but you can't access the buffer 
    // after it has been popped anyway, so who cares if you delete 
    // it or not?  It is also confusingly named.  More research may 
    // be necessary...
    return _erase;
  }

  /**
   * Returns the chunk size.
   */
  public int getChunkSize()
  {
    return _chunkSize;
  }

  /**
   * Cleans (clears) the buffer.
   */
  public void clean()
  {
    try {
      _out.flush();

      _tempStream.clearWrite();
    } catch (IOException e) {
      _env.error(e.toString(), e);
    }
  }

  /**
   * Flushes the data without calling the callback.
   */
  private void doFlush()
  {
    try {
      _out.flush();

      ReadStream rs = _tempStream.openRead(true);

      WriteStream out;

      if (_next != null)
        out = _next.getOut();
      else
        out = _env.getOriginalOut();

      rs.writeToStream(out);

      rs.close();
    } catch (IOException e) {
      _env.error(e.toString(), e);
    }
  }

  /**
   * Invokes the callback using the data in the current buffer.
   */
  private void callCallback()
  {
    if (_callback != null) {
      Value result = 
        _callback.call(_env, getContents(), LongValue.create(_state));

      // special code to do nothing to the buffer
      if (result.isNull())
        return;
      
      clean();

      try {
        if (result instanceof BinaryValue)
          _out.write(((BinaryValue) result).toBytes());
        else
          _out.print(result.toString(_env).toString());
      } catch (IOException e) {
        _env.error(e.toString(), e);
      }
    }
  }

  /**
   * Flushs the data in the stream, calling the callback with appropriate
   * flags if necessary.
   */
  public void flush()
  {
    _state |= 1 << PHP_OUTPUT_HANDLER_CONT;

    callCallback();

    // clear the start and cont flags
    _state &= ~(1 << PHP_OUTPUT_HANDLER_START);
    _state &= ~(1 << PHP_OUTPUT_HANDLER_CONT);

    doFlush();

    _haveFlushed = true;
  }

  /**
   * Closes the output buffer.
   */
  public void close()
  {
    _state |= 1 << PHP_OUTPUT_HANDLER_END;

    callCallback();
    
    // all data that has and ever will be written has now been processed
    _state = 0; 

    doFlush();
  }

  /**
   * Returns the callback for this output buffer.
   */
  public Callback getCallback()
  {
    return _callback;
  }

  /**
   * Sets the callback for this output buffer.
   */
  public void setCallback(Callback callback)
  {
    _callback = callback;
  }
}

