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

import java.io.*;
import java.util.*;

import com.caucho.util.L10N;

public class WidgetMode
{
  private static L10N L = new L10N( WidgetMode.class );

  public final static WidgetMode VIEW = new WidgetMode("view");
  public final static WidgetMode EDIT = new WidgetMode("edit");
  public final static WidgetMode HIDDEN = new WidgetMode("hidden");

  private String _name;

  public WidgetMode( String name ) 
  {
    if (name == null ) {
      throw new IllegalArgumentException("WidgetMode name can not be NULL");
    }

    _name = name.toLowerCase();
  }

  public WidgetMode() 
  {
  }

  public void addText( String name )
  {
    if ( _name != null )
      throw new IllegalArgumentException( 
          L.l("`{0}' already specified", "name" ) );

    _name = name.toLowerCase();
  }

  public void init()
  {
    if ( _name == null )
      throw new IllegalStateException( L.l( "`{0}' is required", "name" ) );
  }

  public String toString() 
  {
    return _name;
  }

  public int hashCode() 
  {
    return _name.hashCode();
  }

  public boolean equals( Object o ) 
  {
    if ( o instanceof WidgetMode ) {
      WidgetMode other = (WidgetMode) o;

      return _name == other._name || _name.equals( other._name );
    } else
      return false;
  }
}

