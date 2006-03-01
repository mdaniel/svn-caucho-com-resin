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


package com.caucho.portal.alpharenderer;

import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import java.io.PrintWriter;

abstract public class Menu
{
  private static L10N L = new L10N( Menu.class );

  static protected final Logger log = 
    Logger.getLogger( Menu.class.getName() );

  private Location _location = Location.HEADER;
  private int _showMin = 1;

  public Menu()
  {
  }

  /**
   * The minimum number of items that must be in the menu for
   * it to be shown, default is 2.
   */
  public void setShowMin( int showMin )
  {
    _showMin = showMin;
  }

  /**
   * One of "hidden", "frame", "header", "footer"; default "header".
   */
  public void setLocation( String location )
  {
    _location =  Location.getLocation( location );
  }

  public Location getLocation()
  {
    return _location;
  }

  public void init()
  {
  }

  public MenuRenderer createRenderer()
  {
    return new MenuRenderer( this );
  }

  abstract protected void menuStart( StringBuffer buf );

  abstract protected void menuItem( StringBuffer buf, 
                                    int count, 
                                    String name, 
                                    String shortDescription, 
                                    String url,
                                    boolean isSelected );

  abstract protected void menuEnd( StringBuffer buf, int count );

  static public class MenuRenderer
  {
    private Menu _menu;
    private int _count;

    private StringBuffer _buf = new StringBuffer();

    MenuRenderer( Menu menu ) 
    {
      _menu = menu;
    }

    public void add( String title, 
                     String shortDescription, 
                     String url, 
                     boolean isSelected )
    {
      if ( _count == 0 ) {
        _menu.menuStart( _buf );
      }

      _count++;

      _menu.menuItem( _buf, _count, title, shortDescription, url, isSelected );
    }

    public void print( PrintWriter out )
      throws IOException
    {
      if ( _count >= _menu._showMin ) {
        int len = _buf.length();
        _menu.menuEnd( _buf, _count );

        out.print( _buf );

        _buf.setLength( len );
      }
    }

    public String toString()
    {
      String toString = "";

      if ( _count >= _menu._showMin ) {
        int len = _buf.length();
        _menu.menuEnd( _buf, _count );

        toString = _buf.toString();

        _buf.setLength( len );
      }

      return toString;
    }

  }

  protected static void appendEscaped( StringBuffer buf, String string )
  {
    if ( string == null ) {
      buf.append( string );
      return;
    }

    for ( int i = 0; i < string.length(); i++  ) {
      char ch = string.charAt( i );

      switch ( ch ) {
        case '<': buf.append( "&lt;" ); break;
        case '>': buf.append( "&gt;" ); break;
        case '&': buf.append( "&amp;" ); break;
                  case '\"': buf.append( "&quot;" ); break;
        case '\'': buf.append( "&rsquo;" ); break;
        default: buf.append( ch );
      }
    }
  }
}

