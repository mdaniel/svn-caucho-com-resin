/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.io.IOException;

/**
 * Stream encapsulating System.out.
 */
public class StdoutStream extends StreamImpl {
  private static StdoutStream _stdout;

  /**
   * Private, since StdoutStream should always use the create() interface.
   */
  private StdoutStream()
  {
  }

  /**
   * Returns the StdoutStream singleton
   */
  public static StdoutStream create()
  {
    if (_stdout == null) {
      _stdout = new StdoutStream();
      ConstPath path = new ConstPath(null, _stdout);
      path.setScheme("stdout");
      _stdout.setPath(path);
    }

    return _stdout;
  }

  /**
   * The standard-output stream returns true since it's writable.
   */
  public boolean canWrite()
  {
    return true;
  }

  /**
   * Writes the data to the System.out.
   *
   * @param buf the buffer to write.
   * @param offset starting offset in the buffer.
   * @param length number of bytes to write.
   * @param isEnd true when the stream is closing.
   */
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    System.out.write(buf, offset, length);
    System.out.flush();
  }
}
