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

public class WidgetPreferences
{
  private static L10N L = new L10N( WidgetPreferences.class );

  static protected final Logger log = 
    Logger.getLogger( WidgetPreferences.class.getName() );

  private Map<String, WidgetPreference> _preferenceMap;

  public void addPreference( WidgetPreference widgetPreference )
  {
    String name = widgetPreference.getName();

    if ( name == null )
      throw new IllegalStateException( L.l( "`{0}' is requried", "name" ) );

    if ( _preferenceMap == null )
      _preferenceMap = new LinkedHashMap<String,WidgetPreference>();

    _preferenceMap.put( name, widgetPreference );
  }

  public WidgetPreference get( String key )
  {
    return _preferenceMap == null ? null : _preferenceMap.get( key );
  }

  public void put( String name, String value )
  {
    WidgetPreference widgetPreference = get( name );

    if ( widgetPreference == null ) {
      widgetPreference = new WidgetPreference();
      widgetPreference.setName( name );
      widgetPreference.addValue( value );
      addPreference( widgetPreference );
    }
    else
      widgetPreference.addValue( value );
  }
}
