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
 * @author Sam
 */


package com.caucho.portal;

import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;

public class PortletMediator
{
  private static L10N L = new L10N( PortletMediator.class );

  static protected final Logger log = 
    Logger.getLogger( PortletMediator.class.getName() );

  private String _namespace;
  private PortletRequest _request;
  private PortletResponse _response;

  public PortletMediator()
  {
  }

  public void setNamespace( String namespace )
  {
    _namespace = namespace;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public void setRequest( PortletRequest request )
  {
    _request = request;
  }

  public void setResponse( PortletResponse response )
  {
    _response = response;
  }

  protected PortletRequest getPortletRequest()
  {
    return _request;
  }

  protected PortletResponse getPortletResponse()
  {
    return _response;
  }

  protected RenderRequest getRenderRequest()
  {
    if ( _request instanceof RenderRequest )
      return (RenderRequest) _request;
    else
      return null;
  }

  protected RenderResponse getRenderResponse()
  {
    if ( _response instanceof RenderResponse )
      return (RenderResponse) _response;
    else
      return null;
  }

  public String toString()
  {
    String className = getClass().getName();

    className = className.substring( className.lastIndexOf('.') + 1 );

    return className + '[' + getNamespace() + ']';
  }
}
