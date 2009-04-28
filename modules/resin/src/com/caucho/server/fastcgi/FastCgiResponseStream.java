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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.fastcgi;

import com.caucho.server.connection.AbstractHttpResponse;
import com.caucho.server.connection.ResponseStream;
import com.caucho.server.connection.AbstractResponseStream;
import com.caucho.server.connection.HttpBufferStore;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FastCgiResponseStream extends ResponseStream {
  private static final Logger log
    = Logger.getLogger(FastCgiResponseStream.class.getName());
  
  private static final L10N L = new L10N(FastCgiResponseStream.class);

  private FastCgiRequest _request;

  FastCgiResponseStream(AbstractHttpResponse response)
  {
    super(response);//, rawStream);
  }

  void setRequest(FastCgiRequest request)
  {
    _request = request;

    if (request == null)
      throw new NullPointerException();
  }

  @Override
  protected void writeTail(int bufferStart)
    throws IOException
  {
    flushBuffer();
    
    _request.writeTail();
  }
}
