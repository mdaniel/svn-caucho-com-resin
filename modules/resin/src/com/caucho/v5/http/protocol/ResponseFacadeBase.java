/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import java.io.IOException;

import com.caucho.v5.http.protocol2.OutHeader;
import com.caucho.v5.vfs.WriteStream;


/**
 * User facade for http responses
 */
public class ResponseFacadeBase
  implements ResponseFacade
{
  @Override
  public void setStatus(int scNotModified)
  {
  }

  @Override
  public int getStatus()
  {
    return 0;
  }

  @Override
  public String getStatusMessage()
  {
    return null;
  }

  @Override
  public String getContentType()
  {
    return null;
  }

  @Override
  public void setContentType(String value)
  {
  }

  @Override
  public String getContentTypeImpl()
  {
    return getContentType();
  }

  @Override
  public String getCharacterEncodingImpl()
  {
    return null;
  }
  
  //
  // caching
  //

  @Override
  public void killCache()
  {
  }

  @Override
  public boolean isCaching()
  {
    return false;
  }

  @Override
  public boolean isNoCache()
  {
    return false;
  }

  @Override
  public boolean isPrivateCache()
  {
    return false;
  }

  @Override
  public void setCacheControl(boolean isCacheControl)
  {
  }

  @Override
  public boolean isCacheControl()
  {
    return false;
  }

  @Override
  public boolean isNoCacheUnlessVary()
  {
    return false;
  }

  @Override
  public boolean handleNotModified() throws IOException
  {
    return false;
  }
  
  //
  // tail callbacks

  @Override
  public void fillHeaders()
  {
  }

  @Override
  public void sendError(int status)
    throws IOException
  {
    setStatus(status);
  }
  
  //
  // http response
  //

  @Override
  public void writeCookies(WriteStream os) throws IOException
  {
  }

  @Override
  public void fillCookies(OutHeader out) throws IOException
  {
  }
}
