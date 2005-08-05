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

import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Widget stores request specific state (S).
 */
abstract public class Widget<S extends WidgetState>
  extends AbstractMap<String, Widget>
{
  private static L10N L = new L10N( Widget.class );

  static protected final Logger log =
    Logger.getLogger( Widget.class.getName() );

  private String _id;
  private String _clientId;
  private String _parameterName;
  private String _modeParameterName;
  private String _preferencePrefix;
  private String _cssClass;
  private WidgetPreferences _widgetPreferences;
  private WidgetMode _widgetMode;
  private HashSet<WidgetMode> _allowedWidgetModesSet;

  private Widget _parent;

  private Lifecycle _lifecycle = new Lifecycle();

  private RendererCache _rendererCache = new RendererCache();


  public Widget()
  {
  }

  public Widget( String id )
  {
    setId( id );
  }

  public Widget( Widget parent )
  {
    setParent( parent );
    parent.put( null, this );
  }

  public Widget( Widget parent, String id )
  {
    setParent( parent );
    setId( id );
    parent.put( getId(), this );
  }

  public void setParent( Widget parent )
  {
    _parent = parent;
  }

  public Widget getParent()
  {
    return _parent;
  }

  public void setId( String id )
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  protected String calculateId( Widget child )
  {
    return "x" + size();
  }

  /**
   * Default is a concatentation of all parent id's separated
   * by `_'.
   */
  public void setClientId( String clientId )
  {
    _clientId = clientId;
  }

  public String getClientId()
  {
    if ( _clientId == null ) {
      if ( _parent == null )
        _clientId = getId();
      else {
        StringBuffer buf = new StringBuffer();

        appendId( buf, _parent, '_' );
        buf.append( getId() );

        _clientId = buf.toString();
      }
    }

    return _clientId;
  }

  private static void appendId( StringBuffer buf, Widget w, char sep )
  {
    if ( w == null )
      return;

    if ( w._parent != null ) {
      appendId( buf, w._parent, sep );
    }

    if ( w.getId() != null ) {
      buf.append( w.getId() );
      buf.append( sep );
    }
  }

  /**
   * Default is a concatentation of all parent id's separated
   * by `.'.
   */
  public void setParameterName( String parameterName )
  {
    _parameterName = parameterName;
  }

  public String getParameterName()
  {
    if ( _parameterName == null ) {
      if ( _parent == null )
        _parameterName = getId();
      else {
        StringBuffer buf = new StringBuffer();

        appendId( buf, _parent, '.' );
        buf.append( getId() );

        _parameterName = buf.toString();
      }
    }

    return _parameterName;
  }

  /**
   * Set a css class for renders that use it, the default is
   * to use the value of "<code>getId()</code> <code>shortClassName</code>"
   * where <i>shortClassName</i> is the classname portion (no package) of the
   * widget's class.
   *
   * If the passed string starts with "+", for example "+clear",
   * then the string is appended to the current cssClass.
   *
   * If the passed string starts with "-", for example "-clear",
   * then the string is removed from the current cssClass.
   *
   * For example, a TextWidget with id "phoneNumber" and clientId
   * "form.phoneNumber" will have a default cssClass of
   * "textWidget * phoneNumber".
   */
  public void setCssClass( String cssClass )
  {
    boolean add =  cssClass.charAt( 0 ) == '+';
    boolean del =  cssClass.charAt( 0 ) == '-';

    if ( add || del ) {
      cssClass = cssClass.substring(1);
      String current = getCssClass();

      StringBuffer cssClassBuf = new StringBuffer();

      if ( current != null && current.length() > 0 ) {
        String[] split = current.split( "\\s*" );

        for ( int i = 0; i < split.length; i++ ) {
          String token = split[i];

          if ( token.equals( cssClass ) )
            continue;

          cssClassBuf.append( token );
        }
      }

      if ( add )
        cssClassBuf.append( cssClass );

      _cssClass = cssClassBuf.toString();
    }
    else {
      _cssClass = cssClass;
    }
  }

  /**
   * Used for information purposes
   */
  public String getLogId()
  {
    StringBuffer buf = new StringBuffer();

    String classname = getClass().getName();
    classname = classname.substring( classname.lastIndexOf('.') + 1);
    buf.append( classname );
    buf.append('[');

    appendLogId( buf, this );

    if ( _parent == null )
      buf.append('/');

    buf.append(']');

    return buf.toString();
  }

  private static void appendLogId( StringBuffer buf, Widget w )
  {
    if ( w._parent != null ) {
      appendLogId( buf, w._parent );
      buf.append( '/' );
    }

    if ( w.getId() != null )
      buf.append( w.getId() );
  }


  /**
   * Used by implementing classes to get a css class
   * appropriate for this widget.
   */
  public String getCssClass()
  {
    if ( _cssClass == null ) {
      StringBuffer buf = new StringBuffer();

      String shortName = getClass().getName();

      int i = shortName.lastIndexOf( '.' ) + 1;

      for ( ; i < shortName.length(); i++ )
        buf.append( shortName.charAt( i ) );

      buf.append(' ');
      buf.append( getId() );

      _cssClass =  buf.toString();
    }

    return _cssClass;
  }

  /**
   * Default is to the parameterName with a prefix of "_" and a suffix
   * of "_".
   */
  public void setModeParameterName( String modeParameterName )
  {
    _modeParameterName = modeParameterName;
  }

  public String getModeParameterName()
  {
    if ( _modeParameterName == null )
      _modeParameterName = "_" + getParameterName() + "_";

    return _modeParameterName;
  }

  public WidgetPreferences createWidgetPreferences()
  {
    if ( _widgetPreferences == null ) {
      _widgetPreferences = new WidgetPreferences();
    }

    return _widgetPreferences;
  }

  public void addPreference( WidgetPreference widgetPreference )
  {
    WidgetPreferences widgetPreferences = createWidgetPreferences();

    widgetPreferences.addPreference( widgetPreference );
  }

  /**
   * If a preference "pref" is not found as a preference
   * specifically set for this widget, the prefix is prepended
   * and the connection preferences are checked; the default is "<i>id.</i>",
   * for example connection.getPreferenceValue("<i>id.</i>pref").
   */
  public void setPreferencePrefix( String preferencePrefix )
  {
    _preferencePrefix = preferencePrefix;
  }

  /**
   * Return a preference value.
   */
  public String getPreferenceValue( WidgetConnection connection, String key )
  {
    String[] values = getPreferenceValues( connection, key );

    if ( values == null )
      return null;
    else if ( values.length == 0 )
      return "";
    else
      return values[0];
  }

  /**
   * Return preference values.
   */
  public String[] getPreferenceValues( WidgetConnection connection, String key )
  {
    WidgetPreference widgetPreference = getWidgetPreference( key );

    boolean isReadOnly = widgetPreference != null
                         && !widgetPreference.isReadOnly();

    String[] values = null;

    if ( widgetPreference != null )
        values = widgetPreference.getValues();

    if ( !isReadOnly ) {
      key = key + _preferencePrefix;
      values = connection.getPreferenceValues( key, values );
    }

    return values;
  }

  protected WidgetPreference getWidgetPreference( String key )
  {
    if ( _widgetPreferences == null )
      return null;
    else
      return _widgetPreferences.get( key );

  }

  /**
   * The default mode to use unless it specified in the State,
   * default is WidgetMode.VIEW.
   */
  public void setWidgetMode( WidgetMode widgetMode )
  {
    _widgetMode = widgetMode;
  }

  WidgetMode getWidgetMode()
  {
    return _widgetMode;
  }

  /**
   * Default is to allow all widget modes.
   */
  public void addAllowedWidgetMode( WidgetMode widgetMode )
  {
    if ( _allowedWidgetModesSet == null )
      _allowedWidgetModesSet = new HashSet<WidgetMode>();

    _allowedWidgetModesSet.add( widgetMode );
  }

  public boolean isWidgetModeAllowed( WidgetMode widgetMode )
  {
    if ( _allowedWidgetModesSet == null )
      return true;
    else
      return _allowedWidgetModesSet.contains( widgetMode );
  }

  public void init()
    throws WidgetException
  {
  }

  private void initAll()
    throws WidgetException
  {
    if ( !_lifecycle.toInitializing() )
      return;

    if ( _id == null && _parent != null )
        throw new IllegalStateException( L.l( "`{0}' is required", "id" ) );

    if ( _preferencePrefix == null )
      _preferencePrefix = _id + ".";

    init();

    _lifecycle.toActive();
  }

  public Set<Map.Entry<String,Widget>> entrySet()
  {
    return (Set<Map.Entry<String,Widget>>) Collections.EMPTY_SET;
  }

  public Widget put( String id, Widget value )
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Called once for each request to restore the state of the widget
   * for the request.
   */
  protected S decode( WidgetConnection connection )
    throws WidgetException
  {
    if ( log.isLoggable( Level.FINER ) )
      log.finer( L.l( "decode {0}",  getLogId() ) );

    S state = createState( connection );
    state.setWidget( this );

    String parameterName = getParameterName();

    if ( parameterName == null && getParent() != null )
      throw new IllegalStateException(
          L.l("`{0}' cannot be null", "parameter-name" ) );

    if ( parameterName != null ) {
      String[] data = connection.getParameterValues( getParameterName() );

      if ( log.isLoggable( Level.FINEST ) )
        log.finest( L.l( "data from parameter {0} is {1}",
                         getParameterName(), data ) );

      state.decode( data );
    }

    decodeChildren( connection, state );

    return state;
  }

  protected void decodeChildren( WidgetConnection connection,
                                 WidgetState state )
    throws WidgetException
  {
    if ( !isEmpty() ) {
      for ( Widget child : values() ) {
        decodeChild( connection, state, child );
      }
    }
  }

  protected WidgetState decodeChild( WidgetConnection connection,
                                     WidgetState thisState,
                                     Widget child )
    throws WidgetException
  {
    WidgetState childState = child.decode( connection );
    childState.setParent( thisState );

    thisState.put( child.getId(), childState );

    return childState;
  }

  abstract protected S createState( WidgetConnection connection )
    throws WidgetException;

  private void encodeTree( WidgetURL url,
                           WidgetState state,
                           Widget target )
    throws WidgetException
  {
    encodeTree( url, state, target, false, false );
  }

  private void encodeTree( WidgetURL url,
                           WidgetState state,
                           Widget target,
                           boolean isAction,
                           boolean sawTarget )
    throws WidgetException
  {
    if ( target == this ) {
      sawTarget = true;
      isAction = isAction( state );
    }

    encode( url, state, isAction );

    if ( !isEmpty() ) {
      for ( Widget child : values() ) {
        WidgetState childState = state.get( child.getId() );

        if ( childState == null ) {
          throw new IllegalStateException(
              L.l( "no state for `{0}', missing decode()?",
                   child.getLogId() ) );
        }

        child.encodeTree( url, childState, target, isAction, sawTarget );
      }
    }
  }

  protected void encode( WidgetURL url,
                         WidgetState state,
                         boolean isAction )
    throws WidgetException
  {
    if ( log.isLoggable( Level.FINEST ) )
      log.finest( L.l( "encode {0}",  getLogId() ) );

    WidgetMode widgetMode = state._widgetMode;

    if ( widgetMode != null ) {
      if ( widgetMode == WidgetMode.VIEW
          || widgetMode.equals( WidgetMode.VIEW ) )
      {
        if ( state.getParent() != null )
          url.setParameter( getModeParameterName(), widgetMode.toString() );
      }
      else {
        url.setParameter( getModeParameterName(), widgetMode.toString() );
      }
    }

    if ( isAction && isActionParameter( state ) ) {
      url.setAction( true );
    }
    else {
      String[] data = state.encode();

      if ( data != null )
        url.setParameter( getParameterName(), data );
    }
  }

  /**
   * Does this widget submit a value directly, s
   * in some manner other than a parameter to the url?
   *
   * This value only has an effect if a url is being made
   * from a widget that has a true value
   * for isAction().
   *
   * For example, HTML fields like &lt;input&gt; return true.
   */
  protected boolean isActionParameter( WidgetState state )
  {
    return false;
  }

  /**
   * Does this widget make it so that children with isActionParameter()
   * can submit values without encoding them?
   *
   * For example, an HTML &lt;form&gt; does this.
   */
  protected boolean isAction( WidgetState state )
  {
    return false;
  }

  /**
   * Derived classes use this to create a url.
   *
   * The widget tree is followed parent by parent until the top is reached,
   * and then the connection is used to create a url for that widget.
   */
  protected WidgetURL createURL( WidgetConnection connection )
    throws WidgetException
  {
    Widget target = this;
    Widget top = this;

    while ( top.getParent() != null )
      top = top.getParent();

    WidgetURL url = connection.createURL();
    WidgetState state = connection.prepare( top );

    top.encodeTree( url, state, target );

    return url;
  }

  public void render( WidgetConnection connection, S widgetState )
    throws WidgetException, IOException
  {
    if ( ! _lifecycle.isActive() )
      initAll();

    WidgetRenderer<S> widgetRenderer
      = _rendererCache.getRenderer( connection, this, widgetState );

    widgetRenderer.render( connection, widgetState );
  }

  public void destroy()
  {
    if ( !_lifecycle.toDestroying() )
      return;

    _lifecycle.toDestroy();
  }
}
