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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The state for a widget is restored from and saved to a String[].
 */
abstract public class WidgetState
  extends AbstractMap<String, WidgetState>
{
  private Widget _widget;
  private WidgetState _parent;
  WidgetMode _widgetMode; // package access for Widget;

  protected WidgetState()
  {
  }

  void setWidget( Widget widget )
  {
    _widget = widget;
  }

  public Widget getWidget()
  {
    return _widget;
  }

  void setParent( WidgetState parent )
  {
    _parent = parent;
  }

  public WidgetState getParent()
  {
    return _parent;
  }

  public Set<Map.Entry<String,WidgetState>> entrySet()
  {
    return (Set<Map.Entry<String,WidgetState>>) Collections.EMPTY_SET;
  }

  public WidgetState put( String id, WidgetState value ) 
  {
    throw new UnsupportedOperationException();
  }

  public Object getValue() 
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Called once for each request to restore the state of the widget
   * for the request.
   */
  abstract public void decode( String[] data )
    throws WidgetException;

  abstract public String[] encode()
    throws WidgetException;

  public List<WidgetWarning> getWarnings()
  {
    return null;
  }

  public List<WidgetError> getErrors()
  {
    return null;
  }

  /**
   * Set the mode for this widget.  The default is to use the mode set for the
   * widget, or if that has not been set to inherit the mode from the parent
   * state.
   */
  public void setWidgetMode( WidgetMode widgetMode )
  {
    if ( _widget.isWidgetModeAllowed( widgetMode ) )
      _widgetMode = widgetMode;
  }

  /**
   * If the widgetMode has not been set, then the parent's mode is returned, if
   * there is no parent then WidgetMode.VIEW is returned.
   */
  final public WidgetMode getWidgetMode()
  {
    WidgetMode widgetMode = _widgetMode;

    if ( widgetMode == null ) {
      widgetMode = getWidget().getWidgetMode();

      if ( widgetMode == null ) {
        if ( _parent != null )
          widgetMode = _parent.getWidgetMode();

        if ( widgetMode == null )
          widgetMode = WidgetMode.VIEW;
      }
    }

    return widgetMode;
  }

  void resetAll()
  {
    reset();

    _widget = null;
    _parent = null;
    _widgetMode = null;
  }

  /**
   * Reset all member variables to the state immediately following 
   * construction.
   */
  abstract public void reset();
}
