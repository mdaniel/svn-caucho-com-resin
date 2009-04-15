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

package com.caucho.server.connection;

import com.caucho.vfs.WriteStream;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Locale;

// Is there anything at all useful that could be put here?
public class StubServletResponse extends AbstractHttpResponse {
  public StubServletResponse()
  {
    try {
      startRequest(null);
    } catch (Throwable e) {
    }
  }
  
  public String getCharacterEncoding()
  {
    return "ISO-8859-1";
  }
  
  public void setLocale(Locale locale)
  {
  }
  
  public Locale getLocale()
  {
    return null;
  }
  
  public boolean writeHeadersInt(WriteStream out, int length, boolean isHead)
  {
    return false;
  }

  public void setBufferSize(int size)
  {
  }
  
  public int getBufferSize()
  {
    return 0;
  }
  
  public void flushBuffer()
  {
  }
  
  public boolean isCommitted()
  {
    return false;
  }
  
  public void reset()
  {
  }
  
  public void resetBuffer()
  {
  }
  
  public void setContentLength(int length)
  {
  }
  
  public void setContentType(String type)
  {
  }

  public void setStatus(int status)
  {
  }
  
  public void setStatus(int status, String messages)
  {
  }
  
  public void sendRedirect(String location)
  {
  }
  
  public void sendError(int i)
  {
  }
  
  public void sendError(int i, String message)
  {
  }
    
  public String encodeUrl(String url)
  {
    return url;
  }
  
  public String encodeURL(String url)
  {
    return url;
  }
  
  public String encodeRedirectUrl(String url)
  {
    return url;
  }
  
  public String encodeRedirectURL(String url)
  {
    return url;
  }

  public void addCookie(Cookie cookie)
  {
  }
  
  public boolean containsHeader(String header)
  {
    return false;
  }
  
  public void setHeader(String header, String value)
  {
  }
  
  public void setIntHeader(String header, int value)
  {
  }
  
  public void setDateHeader(String header, long value)
  {
  }
  
  public void addHeader(String header, String value)
  {
  }
  
  public void addIntHeader(String header, int value)
  {
  }
  
  public void addDateHeader(String header, long value)
  {
  }

  public String getHeader(String key)
  {
    return null;
  }
  
  public void clearBuffer()
  {
  }
  
  public void close() throws IOException
  {
  }

  public boolean disableHeaders(boolean disable)
  {
    return false;
  }

  public int getRemaining()
  {
    return 0;
  }

  public void setForbidForward(boolean forbid)
  {
  }
  
  public boolean getForbidForward()
  {
    return false;
  }
  
  public void setHasError(boolean hasError)
  {
  }
  
  public boolean hasError()
  {
    return true;
  }
  
  public void killCache()
  {
  }
  
  public void setPrivateCache(boolean isPrivate)
  {
  }
  
  public void setSessionId(String id)
  {
  }
}
