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

import java.util.Locale;
import java.util.Collection;

import java.io.IOException;

public class FieldWidget
  extends WidgetContainer
{
  private static L10N L = new L10N( FieldWidget.class );

  static protected final Logger log = 
    Logger.getLogger( FieldWidget.class.getName() );

  private LocaleString _displayName;
  private LocaleString _shortDescription;

  public FieldWidget()
  {
  }

  public FieldWidget( String id )
  {
    super( id );
  }

  public FieldWidget( Widget parent )
  {
    super( parent );
  }

  public FieldWidget( Widget parent, String id )
  {
    super( parent, id );
  }

  /**
   * The name to display
   */
  public void setDisplayName( String displayName )
  {
    _displayName = LocaleString.add( _displayName, displayName );
  }

  /**
   * The name to display
   */
  public void addDisplayName( LocaleString displayName )
  {
    _displayName = LocaleString.add( _displayName, displayName );
  }

  /**
   * The name to display
   */
  public String getDisplayName( Locale locale )
  {
    return LocaleString.get( _displayName, locale );
  }

  /**
   * The name to display
   */
  public String getDisplayName()
  {
    return LocaleString.get( _displayName );
  }

  /**
   * A short description, up to one sentence in length.
   */
  public void setShortDescription( String shortDescription )
  {
    _shortDescription = LocaleString.add( _shortDescription, shortDescription );
  }

  /**
   * A short description, up to one sentence in length.
   */
  public void addShortDescription( LocaleString shortDescription )
  {
    _shortDescription = LocaleString.add( _shortDescription, shortDescription );
  }

  /**
   * A short description, up to one sentence in length.
   */
  public String getShortDescription()
  {
    return LocaleString.get( _shortDescription );
  }

  /**
   * A short description, up to one sentence in length.
   */
  public String getShortDescription( Locale locale )
  {
    return LocaleString.get( _shortDescription, locale );
  }

  protected String calculateId( Widget child )
  {
    String id = null;

    int size = size();

    if ( size == 0 )
      id = "value";
    else
      id = "value" + size;

    return id;
  }

  public void init()
    throws WidgetException
  {
    super.init();

    if ( getDisplayName() == null )
      throw new IllegalStateException( 
          L.l( "`{0}' is required", "display-name" ) );
  }

  protected FieldWidgetState createState( WidgetConnection connection )
    throws WidgetException
  {
    return new FieldWidgetState();
  }

  protected WidgetState decodeChild( WidgetConnection connection,
                                     WidgetState thisState,
                                     Widget child )
    throws WidgetException
  {
    WidgetState childState 
      = super.decodeChild( connection, thisState, child );

    FieldWidgetState state = (FieldWidgetState) thisState;

    state.setChildWithValue( childState );

    return childState;
  }

  public void renderTextHtml( WidgetConnection connection,
                              FieldWidgetState widgetState )
    throws WidgetException, IOException
  {
    WidgetWriter writer = connection.getWriter();

    Locale locale = connection.getLocale();

    String displayName = getDisplayName( locale );
    String shortDescription = getShortDescription( locale );

    writer.startElement( "div", true );
    writer.writeAttribute( "id", getClientId() );
    writer.writeAttribute( "class", getCssClass() );

    // name

    writer.startElement( "div", true );
    writer.writeAttribute( "id", getClientId() );
    writer.writeAttribute( "class", "name" );

    if ( shortDescription != null )
      writer.writeAttribute( "title", shortDescription );

    if ( displayName != null )
      writer.writeText( displayName );

    writer.endElement( "div", true ); 


    // XXX:
    // warnings
    // errors

    // value

    writer.startElement( "div", true );          
    writer.writeAttribute( "id", getClientId() );
    writer.writeAttribute( "class", "value" );

    super.renderChildren( connection, widgetState );

    writer.endElement( "div", true ); 

    //

    writer.endElement( "div", true ); 
  }
}
