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

import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import java.io.IOException;

/**
 * A Widget that contains other widgets.
 * If the WidgetContainer has no parent, then
 * this Widget stores the state of children
 * as request attributes that correspond to their id's.
 */
public class WidgetContainer
  extends Widget
{
  private static L10N L = new L10N( WidgetContainer.class );

  static protected final Logger log = 
    Logger.getLogger( WidgetContainer.class.getName() );

  private String _attributePrefix;
  private Map<String,Widget> _childMap;

  public WidgetContainer()
  {
  }

  public WidgetContainer( String id )
  {
    super( id );
  }

  public WidgetContainer( Widget parent )
  {
    super( parent );
  }

  public WidgetContainer( Widget parent, String id )
  {
    super( parent, id );
  }

  /**
   * Prefix to use for request attribute's of children when this is a toplevel
   * container, default is null which means no prefix.
   */
  public void setAttributePrefix( String attributePrefix )
  {
    _attributePrefix = attributePrefix;
  }

  public Set<Map.Entry<String,Widget>> entrySet()
  {
    if ( _childMap == null )
      return super.entrySet();
    else
      return _childMap.entrySet();
  }

  public boolean isEmpty()
  {
    return _childMap == null ? true :  _childMap.isEmpty();
  }

  public Widget put( String id, Widget value ) 
  {
    if ( _childMap == null )
      _childMap = new LinkedHashMap<String,Widget>();

    if ( id == null )
      id = calculateId( value );

    if ( (id == null && value.getId() != null )
         || ( !id.equals( value.getId() ) ) )
      value.setId( id );

    return _childMap.put( id, value );
  }

  public Widget remove( String id ) 
  {
    if ( _childMap == null )
      return null;
    else
      return _childMap.remove( id );
  }

  protected WidgetContainerState createState( WidgetConnection connection)
    throws WidgetException
  {
    return new WidgetContainerState();
  }

  protected WidgetState decodeChild( WidgetConnection connection,
                                     WidgetState thisState,
                                     Widget child )
    throws WidgetException
  {
    WidgetContainerState state =  (WidgetContainerState) thisState;

    WidgetState childState 
      = super.decodeChild( connection, state, child );

    String childId = child.getId();

    if ( state.getParent() == null ) {
      if ( _attributePrefix != null && _attributePrefix.length() > 0 )
        connection.setAttribute( _attributePrefix + childId, childState );
      else
        connection.setAttribute( childId, childState );
    }

    return childState;
  }

  public void render( String contentType, 
                      WidgetConnection connection,
                      WidgetContainerState widgetState )
    throws WidgetException, IOException
  {
    renderChildren( connection, widgetState );
  }

  public void renderChildren( WidgetConnection connection, 
                              WidgetContainerState widgetState )
    throws WidgetException, IOException
  {
    if ( !isEmpty() ) {
      for ( Widget child : (Collection<Widget>) values() ) {
        child.render( connection, widgetState.get( child.getId() ) );
      }
    }
  }
}
