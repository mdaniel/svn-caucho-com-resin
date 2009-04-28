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

import com.caucho.server.connection.AbstractHttpRequest;
import com.caucho.server.connection.AbstractHttpResponse;
import com.caucho.server.connection.AbstractResponseStream;
import com.caucho.server.connection.ResponseStream;
import com.caucho.server.connection.HttpBufferStore;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.WriteStream;

import javax.servlet.http.Cookie;
import java.io.IOException;

/**
 * Handles a response for a srun connection, i.e. a connection to
 * a web server plugin.
 */
public class FastCgiResponse extends AbstractHttpResponse {
  private FastCgiRequest _req;
  
  FastCgiResponse(FastCgiRequest request, WriteStream rawWrite)
  {
    super(request, rawWrite);
    
    _req = request;

    if (request == null)
      throw new NullPointerException();
  }

  /**
   * Return true for the top request.
   */
  public boolean isTop()
  {
    if (! (_request instanceof AbstractHttpRequest))
      return false;
    else {
      return ((AbstractHttpRequest) _request).isTop();
    }
  }

  @Override
  protected AbstractResponseStream createResponseStream()
  {
    FastCgiResponseStream responseStream = new FastCgiResponseStream(this);
    responseStream.setRequest((FastCgiRequest) _request);
    responseStream.init(getRawWrite());

    return responseStream;
  }

  /**
   * headersWritten cannot be undone for hmux
   */
  @Override
  public void setHeaderWritten(boolean isWritten)
  {
    // server/265a
  }
  
  protected boolean writeHeadersInt(WriteStream os,
				    int length,
				    boolean isHead)
    throws IOException
  {
    if (! _originalRequest.hasRequest())
      return false;

    os.print("Status: ");
    os.print(_statusCode);
    os.print(' ');
    os.print(_statusMessage);
    os.print("\r\n");

    CharBuffer cb = _cb;
    
    if (_statusCode >= 400) {
      removeHeader("ETag");
      removeHeader("Last-Modified");
    }
    else if (_isNoCache) {
      removeHeader("ETag");
      removeHeader("Last-Modified");

      setHeader("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");

      os.print("Cache-Control: no-cache\r\n");
    }
    else if (isPrivateCache()) {
      os.print("Cache-Control: private\r\n");
    }

    int size = _headerKeys.size();
    for (int i = 0; i < size; i++) {
      String key = (String) _headerKeys.get(i);
      String value = (String) _headerValues.get(i);

      os.print(key);
      os.print(": ");
      os.print(value);
      os.print("\r\n");
    }

    long now = Alarm.getCurrentTime();
    size = _cookiesOut.size();
    for (int i = 0; i < size; i++) {
      Cookie cookie = (Cookie) _cookiesOut.get(i);
      int cookieVersion = cookie.getVersion();

      fillCookie(cb, cookie, now, 0, false);

      os.print("Set-Cookie: ");
      os.print(cb);
      os.print("\r\n");

      if (cookieVersion > 0) {
        fillCookie(cb, cookie, now, cookieVersion, true);
	
	os.print("Set-Cookie2: ");
	os.print(cb);
	os.print("\r\n");
      }
    }

    if (_contentType != null) {
      if (_charEncoding != null) {
	os.print("Content-Type: ");
	os.print(_contentType);
	os.print("; charset=");
	os.print(_charEncoding);
	os.print("\r\n");
      }
      else {
	os.print("Content-Type: ");
	os.print(_contentType);
	os.print("\r\n");
      }      
    }

    os.print("\r\n");

    return false;
  }

  @Override
  protected void finishResponseStream(boolean isClose)
    throws IOException
  {
    super.finishResponseStream(isClose);

    //_req.finishResponse();
  }
}
