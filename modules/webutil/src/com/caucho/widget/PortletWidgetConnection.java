/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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


package com.caucho.widget;

import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.portlet.*;

public class PortletWidgetConnection
  extends WidgetConnection
{
  private static L10N L = new L10N( PortletWidgetConnection.class );

  static protected final Logger log = 
    Logger.getLogger( PortletWidgetConnection.class.getName() );

  private PortletRequest _portletRequest;
  private PortletResponse _portletResponse;
  private ActionRequest _actionRequest;
  private ActionResponse _actionResponse;
  private RenderRequest _renderRequest;
  private RenderResponse _renderResponse;

  private WidgetWriter _widgetWriter;

  static public <S extends WidgetState> 
    S prepare( PortletRequest request, PortletResponse response, Widget<S> top )
    throws PortletException
  {
    WidgetConnection widgetConnection
      = PortletWidgetConnection.create( request, response );

    try {
      return widgetConnection.prepare( top );
    }
    catch ( WidgetException ex ) {
      throw new PortletException( ex );
    }
  }

  static public <S extends WidgetState> 
    S render( RenderRequest request, RenderResponse response, Widget<S> top )
    throws PortletException, IOException
  {
    WidgetConnection widgetConnection
      = PortletWidgetConnection.create( request, response );

    try {
      return widgetConnection.render( top );
    }
    catch ( WidgetException ex ) {
      throw new PortletException( ex );
    }
  }

  public static PortletWidgetConnection create( PortletRequest request, 
                                                PortletResponse response )
  {
    PortletWidgetConnection connection = new PortletWidgetConnection();

    connection.start( request, response );

    return connection;
  }

  public static PortletWidgetConnection create( ActionRequest request, 
                                                ActionResponse response )
  {
    PortletWidgetConnection connection = new PortletWidgetConnection();

    connection.start( request, response );

    return connection;
  }

  public static PortletWidgetConnection create( RenderRequest request, 
                                                RenderResponse response )
  {
    PortletWidgetConnection connection = new PortletWidgetConnection();

    connection.start( request, response );

    return connection;
  }

  PortletWidgetConnection()
  {
  }

  void start( PortletRequest request, PortletResponse response )
  {
    finish();
    _portletRequest = request;
    _portletResponse = response;
  }

  void start( ActionRequest request, ActionResponse response )
  {
    finish();
    _portletRequest = request;
    _portletResponse = response;
    _actionRequest = request;
    _actionResponse = response;
  }

  void start( RenderRequest request, RenderResponse response )
  {
    finish();
    _portletRequest = request;
    _portletResponse = response;
    _renderRequest = request;
    _renderResponse = response;
  }

  public void finish()
  {
    _widgetWriter = null;
    _portletRequest = null;
    _portletResponse = null;
    _actionRequest = null;
    _actionResponse = null;
    _renderRequest = null;
    _renderResponse = null;
  }

  RenderRequest getRenderRequest()
  {
    return _renderRequest;
  }

  RenderResponse getRenderResponse()
  {
    return _renderResponse;
  }

  public <S extends WidgetState> WidgetURL createURL()
    throws WidgetException
  {
    PortletWidgetURL url = new PortletWidgetURL( this );

    return url;
  }

  public String[] getPreferenceValues( String name, String[] defaults )
  {
    return _portletRequest.getPreferences().getValues( name, defaults );
  }

  public Object getAttribute( String name )
  {
    return _portletRequest.getAttribute( name );
  }

  public void setAttribute( String name, Object object )
  {
    _portletRequest.setAttribute( name, object );
  }

  public void removeAttribute( String name )
  {
    _portletRequest.removeAttribute( name );
  }

  public Enumeration getAttributeNames()
  {
    return _portletRequest.getAttributeNames();
  }

  public String getParameter( String name )
  {
    return _portletRequest.getParameter( name );
  }

  public String[] getParameterValues( String name )
  {
    return _portletRequest.getParameterValues( name );
  }

  public Map getParameterMap()
  {
    return _portletRequest.getParameterMap();
  }

  public Enumeration getParameterNames()
  {
    return _portletRequest.getParameterNames();
  }

  public Locale getLocale()
  {
    if ( _renderResponse == null )
      return _portletRequest.getLocale();
    else
      return _renderResponse.getLocale();
  }

  public String getContentType()
  {
    String contentType;

    if ( _renderResponse == null ) {
      contentType = _portletRequest.getResponseContentType();

      if ( contentType == null )
        contentType = "text/html";
    }
    else {
      contentType =  _renderResponse.getContentType();

      if ( contentType == null ) {
        contentType = "text/html";
        _renderResponse.setContentType( contentType );
      }
    }

    return contentType;
  }

  public String getRemoteUser()
  {
    return _portletRequest.getRemoteUser();
  }

  public java.security.Principal getUserPrincipal()
  {
    return _portletRequest.getUserPrincipal();
  }

  public boolean isUserInRole( String role )
  {
    return _portletRequest.isUserInRole( role );
  }

  public boolean isSecure()
  {
    return _portletRequest.isSecure();
  }

  public String resolveURL( String path )
  {
    return _portletResponse.encodeURL(path);
  }

  public WidgetWriter getWriter()
    throws IOException
  {
    if ( _widgetWriter == null ) {

      if (_renderResponse == null )
        throw new IllegalStateException( L.l( "Writer for portlets can only be obtained during render phase" ) );

      _widgetWriter =  new WidgetWriter( _renderResponse.getWriter() );
    }

    return _widgetWriter;
  }
}
