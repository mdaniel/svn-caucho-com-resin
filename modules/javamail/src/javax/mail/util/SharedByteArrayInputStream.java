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

package javax.mail.util;
import javax.mail.internet.SharedInputStream;
import javax.mail.*;
import java.io.*;

/**
 * A ByteArrayInputStream that implements the SharedInputStream
 * interface, allowing the underlying byte array to be shared between
 * multiple readers.  Since: JavaMail 1.4
 */
public class SharedByteArrayInputStream extends ByteArrayInputStream
  implements SharedInputStream {

  /**
   * Position within shared buffer that this stream starts at.
   */
  protected int start;

  /**
   * Create a SharedByteArrayInputStream representing the entire byte array.
   * buf - the byte array
   */
  public SharedByteArrayInputStream(byte[] buf)
  {
    super(null); // XXX: remove this
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Create a SharedByteArrayInputStream representing the part of the
   * byte array from offset for length bytes.  buf - the byte
   * arrayoffset - offset in byte array to first byte to includelength
   * - number of bytes to include
   */
  public SharedByteArrayInputStream(byte[] buf, int offset, int length)
  {
    super(null); // XXX: remove this
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the current position in the InputStream, as an offset from
   * the beginning of the InputStream.
   */
  public long getPosition()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return a new InputStream representing a subset of the data from
   * this InputStream, starting at start (inclusive) up to end
   * (exclusive). start must be non-negative. If end is -1, the new
   * stream ends at the same place as this stream. The returned
   * InputStream will also implement the SharedInputStream interface.
   */
  public InputStream newStream(long start, long end)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
