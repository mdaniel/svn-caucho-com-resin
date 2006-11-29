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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class RendererCache
{
  private static L10N L = new L10N( RendererCache.class );

  static protected final Logger log = 
    Logger.getLogger( RendererCache.class.getName() );

  static final WidgetRenderer<Object> NULL_RENDERER
    = new WidgetRenderer<Object>(){
      public void render( WidgetConnection connection, 
                          Object widgetState )
        throws WidgetException, IOException
      {
      }
    };

  private Map<String, WidgetRenderer> _rendererCache 
    = Collections.synchronizedMap(new HashMap<String, WidgetRenderer>());

  public <S extends WidgetState>
    WidgetRenderer<S> getRenderer( WidgetConnection connection,
                                   Widget<S> widget,
                                   S widgetState )
  {
    String contentType = connection.getContentType();
    WidgetMode widgetMode = widgetState.getWidgetMode();
    
    String cacheKey = contentType + Character.MAX_VALUE + widgetMode.toString();

    WidgetRenderer renderer = _rendererCache.get( cacheKey );

    if ( renderer != null )
      return (WidgetRenderer<S>) renderer;

    if ( renderer == NULL_RENDERER ) {
      if ( log.isLoggable( Level.FINEST ) )
        log.finest( 
            L.l( "no WidgetRenderer for contentType {0} for widget type {1}",
                 contentType, widget.getClass().getName() ) );

      return null;
    }

    String methodName = createMethodName( contentType );

    Class widgetClass = widget.getClass();
    Class widgetStateClass = widgetState.getClass();

    // XXX: look for a WidgetRenderer added with addRenderer(WidgetRenderer)

    // XXX: look for a globally registered WidgetRenderer in a WidgetRendererManager


    // XXX: this won't work right if getMethod( "foo", { Baz.class } )
    // matches a method foo( Foo ) where Baz extends Bar
    // the createRenderer() methods may need to change

    while ( renderer == null && widgetStateClass != null ) {

      // look for render{ContentType}(WidgetConnection, widgetStateClass )

      renderer 
        = createMethodNameRenderer( widget, widgetClass, widgetStateClass, methodName );

      // look for render(String, WidgetConnection, widgetStateClass )

      if ( renderer == null )
        renderer 
          = createContentTypeRenderer( widget, widgetClass,  widgetStateClass, contentType );

      // try with the superclass of widgetStateClass

      widgetStateClass = widgetStateClass.getSuperclass();
    }

    if ( renderer == null ) {
      if ( log.isLoggable( Level.FINE ) )
        log.fine( 
            L.l( "no WidgetRenderer for contentType {0} for widget type {1}",
                 contentType, widget.getClass().getName() ) );

      renderer = NULL_RENDERER;
    }

    _rendererCache.put( cacheKey, renderer );

    return (WidgetRenderer<S>) renderer;
  }

  /** look for render{ContentType}( WidgetConnection, widgetClass ) */
  private <S extends WidgetState>
    WidgetRenderer<S> createMethodNameRenderer( final Widget<S> widget,
                                                Class widgetClass, 
                                                Class widgetStateClass, 
                                                String methodName )
  {
    Class[] methodArgs 
      = new Class[] { WidgetConnection.class, widgetStateClass };

    Method findMethod = null;

    try {
      findMethod = widgetClass.getMethod( methodName, methodArgs );
    }
    catch (NoSuchMethodException ex) {
    }

    if ( findMethod != null ) {
      if ( log.isLoggable( Level.FINEST ) )
        log.finest( L.l( "WidgetRenderer for widget type {0} is method {1}()",
                    widget.getClass().getName(), methodName ) );

      final Method method = findMethod;

      return new WidgetRenderer<S>() {
        public void render( WidgetConnection connection, S widgetState )
          throws WidgetException, IOException
        {
          try {
            method.invoke( widget, 
                           new Object[] { connection, widgetState });
          }
          catch (IllegalAccessException ex) {
            throw new WidgetException(ex);
          }
          catch (InvocationTargetException ex) {
            throw new WidgetException(ex);
          }
        }
      };
    }
    else 
      return null;
  }

  private <S extends WidgetState>
    WidgetRenderer createContentTypeRenderer( final Widget<S> widget,
                                              Class widgetClass, 
                                              Class widgetStateClass,
                                              final String contentType )
  {
    Class[] methodArgs 
      = new Class[] { String.class, WidgetConnection.class, widgetStateClass };

    Method findMethod = null;

    try {
      findMethod = widgetClass.getMethod( "render", methodArgs );
    }
    catch (NoSuchMethodException ex) {
    }


    if ( findMethod != null ) {
      if ( log.isLoggable( Level.FINEST ) )
        log.finest( L.l( "WidgetRenderer for contentType {0} for widget type {1} is method {2}()", contentType, widget.getClass().getName(), "render" ) );

      final Method method = findMethod;

      return new WidgetRenderer<S>() {
        public void render( WidgetConnection connection, S widgetState )
          throws WidgetException, IOException
        {
          try {
            method.invoke( widget, 
                           new Object[] {contentType, connection, widgetState});
          }
          catch (IllegalAccessException ex) {
            throw new WidgetException(ex);
          }
          catch (InvocationTargetException ex) {
            throw new WidgetException(ex);
          }
        }
      };
    }
    else 
      return null;
  }

  private String createMethodName( String contentType )
  {
    StringBuffer methodName = new StringBuffer();
    methodName.append("render");

    boolean toUpper = true;

    for (int i = 0; i < contentType.length(); i++) {
      char ch = contentType.charAt( i );

      boolean isIdentifier = 
        i == 0 ? Character.isJavaIdentifierStart( ch )
               : Character.isJavaIdentifierPart( ch );

      if ( ! isIdentifier ) {
        if ( toUpper )
          ch = '_';
        else {
          toUpper = true;
          continue;
        }
      }

      if ( toUpper )
        ch = Character.toUpperCase( ch );

      methodName.append( ch );

      toUpper = false;

    }

    return methodName.toString();
  }
}
