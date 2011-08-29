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
import java.util.logging.Logger;

public class FormWidget
  extends WidgetContainer
{
  private static L10N L = new L10N( FormWidget.class );

  static protected final Logger log = 
    Logger.getLogger( FormWidget.class.getName() );

  public FormWidget()
  {
  }

  public FormWidget( String id )
  {
    super( id );
  }

  public FormWidget( Widget parent )
  {
    super( parent );
  }

  public FormWidget( Widget parent, String id )
  {
    super( parent, id );
  }

  protected FormWidgetState createState( WidgetConnection connection )
    throws WidgetException
  {
    return new FormWidgetState();
  }

  protected boolean isAction( WidgetState state )
  {
    return true;
  }

  public void renderTextHtml( WidgetConnection connection,
                              FormWidgetState widgetState )
    throws WidgetException, IOException
  {
    WidgetWriter writer = connection.getWriter();

    writer.startElement( "div", true );
    writer.writeAttribute( "id", getClientId() );
    writer.writeAttribute( "class", getCssClass() );

    writer.startElement( "form", true );
    writer.writeAttribute( "class", getId() );
    writer.writeAttribute( "method", "POST" );
    writer.writeAttribute( "action", createURL( connection ) );

    super.renderChildren( connection, widgetState );

    writer.endElement( "form", true );
    writer.endElement( "div", true );
  }
}
