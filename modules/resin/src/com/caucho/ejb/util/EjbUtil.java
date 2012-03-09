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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */
package com.caucho.ejb.util;

import java.io.IOException;
import java.security.Principal;

import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.security.SecurityContext;
import com.caucho.security.SecurityContextException;
import com.caucho.security.SecurityContextProvider;

/**
 * Manages XA for bean methods.
 */
public class EjbUtil {
  
  public static ProtocolConnection createRequestContext()
  {
    Principal user = SecurityContext.getUserPrincipal();
    
    if (user != null)
      return new AsyncProtocolConnection(user);
    else
      return null;
  }
  
  static class AsyncProtocolConnection
    extends AbstractProtocolConnection implements SecurityContextProvider {
    private Principal _principal;
    
    AsyncProtocolConnection(Principal principal)
    {
      _principal = principal;
    }

    @Override
    public Principal getUserPrincipal() throws SecurityContextException
    {
      return _principal;
    }

    @Override
    public String getProtocolRequestURL()
    {
      return null;
    }

    @Override
    public boolean handleRequest() throws IOException
    {
      return false;
    }

    @Override
    public boolean handleResume() throws IOException
    {
      return false;
    }

    @Override
    public void init()
    {
    }

    @Override
    public boolean isWaitForRead()
    {
      return false;
    }

    @Override
    public void onCloseConnection()
    {
    }

    /* (non-Javadoc)
     * @see com.caucho.network.listen.ProtocolConnection#onStartConnection()
     */
    @Override
    public void onStartConnection()
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public boolean isTransportSecure() throws SecurityContextException
    {
      // TODO Auto-generated method stub
      return false;
    }

    /* (non-Javadoc)
     * @see com.caucho.security.SecurityContextProvider#isUserInRole(java.lang.String)
     */
    @Override
    public boolean isUserInRole(String permission)
    {
      // TODO Auto-generated method stub
      return false;
    }

    /* (non-Javadoc)
     * @see com.caucho.security.SecurityContextProvider#runAs(java.lang.String)
     */
    @Override
    public String runAs(String roleName)
    {
      // TODO Auto-generated method stub
      return null;
    }
    
  }
}