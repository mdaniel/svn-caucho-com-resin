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

package com.caucho.ejb.burlap;

import com.caucho.burlap.io.BurlapInput;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

/**
 * Utility class to call methods easily.
 */
public class MetaStub {
  /**
   * Calls an arbitrary method at the given url.
   *
   * @param url a path to the remote url
   * @param method the method to call
   * @param arg an argument
   */
  public static Object call(Path urlPath, String method, Object arg)
    throws Throwable
  {
    return call(urlPath, method, new Object[] { arg });
  }
  
  /**
   * Calls an arbitrary method at the given url.
   *
   * @param url a path to the remote url
   * @param method the method to call
   * @param args any arguments to call
   */
  public static Object call(Path urlPath, String method, Object []args)
    throws Throwable
  {
    ReadWritePair pair = urlPath.openReadWrite();

    ReadStream is = pair.getReadStream();
    WriteStream os = pair.getWriteStream();
    
    BurlapInput in = new BurlapInput(is);
    BurlapWriter out = new BurlapWriter(os);

    try {
      out.call(method, args);

      String status = (String) is.getAttribute("status");

      if (! "200".equals(status)) {
        CharBuffer msg = new CharBuffer();

        int ch;
        while ((ch = is.readChar()) >= 0)
          msg.append((char) ch);

        throw new IOException("bad status: " + status + "\n" + msg);
      }

      return in.readReply(null);
    } finally {
      os.close();
      is.close();
    }
  }
}
