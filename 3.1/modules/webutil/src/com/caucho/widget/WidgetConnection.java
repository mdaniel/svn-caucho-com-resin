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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

abstract public class WidgetConnection
{
  private static L10N L = new L10N( WidgetConnection.class );

  static protected final Logger log =
    Logger.getLogger( WidgetConnection.class.getName() );

  public <S extends WidgetState> S prepare( Widget<S> top )
    throws WidgetException
  {
    if ( top == null )
      throw new IllegalArgumentException(
          L.l( "`{0}' cannot be null", "top" ) );

    String attributeName = "com.caucho.widget." + System.identityHashCode(top);

    S widgetState = null;
    widgetState = (S) getAttribute( attributeName );

    if ( widgetState == null ) {
      widgetState = top.decode( this );
      setAttribute( attributeName, widgetState );
    }

    return widgetState;
  }

  public <S extends WidgetState> S render( Widget<S> top )
    throws WidgetException, IOException
  {
    S widgetState = prepare( top );

    top.render( this, widgetState );

    return widgetState;
  }

  abstract public <S extends WidgetState> WidgetURL createURL()
    throws WidgetException;

  abstract public String[] getPreferenceValues( String name, String[] defaults );

  abstract public Object getAttribute( String name );

  abstract public void setAttribute( String name, Object object );

  abstract public void removeAttribute( String name );

  abstract public Locale getLocale();

  abstract public String getContentType();

  abstract public Enumeration getAttributeNames();

  abstract public String getParameter(String name);

  abstract public String[] getParameterValues(String name);

  abstract public Map getParameterMap();

  abstract public Enumeration getParameterNames();

  abstract public String getRemoteUser();

  abstract public java.security.Principal getUserPrincipal();

  abstract public boolean isUserInRole(String role);

  abstract public boolean isSecure();

  /**
   * Resolve a url to a resource.
   *
   * The <code>path</code> may be an absolute URL ("http://myserver/...")
   * or a URI with a full path ("/myapp/mypath/....").
   *
   * <code>path</code> may also be a relative path ("images/myimage.gif"), in
   * which case it is a url to a resource in the current servlet or portal.
   *
   * The returned URL is always an absolute url.  Some browsers do not
   * understand relative url's supplied for certain parameters (such as the
   * location of css files).
   *
   * @return an absolute URL
   */
  abstract public String resolveURL( String path );

  abstract public WidgetWriter getWriter()
    throws IOException;

  /**
   * Finish with this connection, allowing it to be reused for a new connection.
   */
  abstract public void finish();
}

