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

import java.util.Map;
import java.util.LinkedHashMap;

import javax.portlet.*;

public class PortletWidgetURL
  extends WidgetURL
{
  private static L10N L = new L10N( PortletWidgetURL.class );

  static protected final Logger log = 
    Logger.getLogger( PortletWidgetURL.class.getName() );

  private PortletWidgetConnection _connection;

  private Map<String,String[]> _parameterMap;
  private PortletMode _portletMode;
  private WindowState _windowState;
  private Boolean _isSecure;
  private boolean _isAction;

  private PortletURL _portletURL;
  private String _url;

  public PortletWidgetURL( PortletWidgetConnection connection )
  {
    _connection = connection;

  }

  public void setParameter( String name, String value )
  {
    String[] values = new String[] { value };

    setParameter( name, values );
  }

  public void setParameter( String name, String[] values )
  {
    if ( _parameterMap == null )
      _parameterMap = new LinkedHashMap<String,String[]>();

    _parameterMap.put( name, values );

    _url = null;

    if ( _portletURL != null )
      _portletURL.setParameter( name, values );
  }

  public void setParameters( Map<String,String[]> parameters )
  {
    if ( _parameterMap == null )
      _parameterMap = new LinkedHashMap<String, String[]>();
    else
      _parameterMap.clear();

    _url = null;

    _parameterMap.putAll( parameters );

    if ( _portletURL != null )
      _portletURL.setParameters( parameters );
  }

  public void setPortletMode( PortletMode portletMode )
    throws PortletModeException
  {
    if ( portletMode.equals( _portletMode ) )
      return;

    _portletMode = portletMode;

    _url = null;

    if ( _portletURL != null )
      _portletURL.setPortletMode( portletMode );
  }

  public void setWindowState( WindowState windowState )
    throws WindowStateException
  {
    if ( windowState.equals( _windowState ) )
      return;

    _windowState = windowState;

    _url = null;

    if ( _portletURL != null )
      _portletURL.setWindowState( windowState );
  }

  public void setSecure( boolean secure ) 
  {
    if ( _isSecure != null && _isSecure.booleanValue() == secure )
      return;

    _isSecure = secure ? Boolean.TRUE : Boolean.FALSE;

    _url = null;

    if ( _portletURL != null ) {
      try {
        _portletURL.setSecure( secure );
      }
      catch ( PortletException ex ) {
        throw new RuntimeException( ex );
      }
    }
  }

  public void setAction( boolean isAction )
  {
    if ( isAction != _isAction) {
      _url = null;
      _portletURL = null;

      _isAction = isAction;
    }
  }

  public String toString()
  {
    if ( _url != null )
      return _url;

    if ( _portletURL != null )
      return _portletURL.toString();

    try {
      RenderResponse response = _connection.getRenderResponse();

      _portletURL = _isAction ? response.createActionURL() 
        : response.createRenderURL();

      if ( _windowState != null )
        _portletURL.setWindowState( _windowState );
      if ( _portletMode != null )
        _portletURL.setPortletMode( _portletMode );

      if ( _isSecure != null )
        _portletURL.setSecure( _isSecure.booleanValue() );

      if ( _parameterMap != null ) {
        for ( Map.Entry<String,String[]> entry : _parameterMap.entrySet() ) {
          _portletURL.setParameter( entry.getKey(), entry.getValue() );
        }
      }
    }
    catch ( PortletException ex ) {
      throw new RuntimeException( ex );
    }

      _url =  _portletURL.toString();

      return _url;
    }
  }
