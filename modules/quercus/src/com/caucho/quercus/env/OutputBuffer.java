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

import java.io.IOException;

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP output buffer
 */
public class OutputBuffer {
  private final OutputBuffer _next;

  private final TempStream _tempStream;
  private final WriteStream _out;

  private final Env _env;
  private final Callback _callback;

  OutputBuffer(OutputBuffer next, Env env, Callback callback)
  {
    _next = next;

    _env = env;
    _callback = callback;

    _tempStream = new TempStream();
    _out = new WriteStream(_tempStream);
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
      StringBuilder sb = new StringBuilder();
      int ch;

      // XXX: encoding
      while ((ch = rs.read()) >= 0) {
	sb.append((char) ch);
      }

      return new StringValue(sb.toString());
    } catch (IOException e) {
      _env.error(e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the buffer length.
   */
  public Value getLength()
  {
    try {
      _out.flush();

      return new LongValue(_tempStream.getLength());
    } catch (IOException e) {
      _env.error(e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the buffer length.
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
   * Closes the output buffer.
   */
  public void close()
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
    } catch (IOException e) {
      _env.error(e.toString(), e);
    }
  }
}

