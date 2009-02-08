/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.hessian;

import com.caucho.hessian.io.*;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.logging.*;

/**
 * Utility class to call methods easily.
 */
public class MetaStub {
  private static final L10N L = new L10N(MetaStub.class);
  private static final Logger log = Logger.getLogger(MetaStub.class.getName());
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
    if (log.isLoggable(Level.FINER)) {
      log.finer("MetaStub[] " + " calling " + method + " on "
		+ urlPath);
    }
    
    ReadWritePair pair = urlPath.openReadWrite();

    ReadStream is = pair.getReadStream();
    WriteStream os = pair.getWriteStream();
    
    Hessian2Input in = new Hessian2Input(is);
    HessianWriter out = new HessianWriter(os);

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

      int code = in.read();
      int major = in.read();
      int minor = in.read();

      if (code != 'H' || major != 2 || minor != 0) {
	throw new IOException(L.l("expected hessian 2.0 header at {0} {1}.{0}",
				  (char) code, major, minor));
      }

      return in.readReply(null);
    } finally {
      os.close();
      is.close();
    }
  }
}
