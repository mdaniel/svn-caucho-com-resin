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


package com.caucho.portal.alpharenderer;

import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;


public class HtmlMenu
  extends Menu
{
  private static L10N L = new L10N( HtmlMenu.class );

  static protected final Logger log = 
    Logger.getLogger( HtmlMenu.class.getName() );


  private String _cssClass;
  private boolean _showSeparators = true;

  public HtmlMenu()
  {
  }

  public void setCssClass( String cssClass )
  {
    _cssClass = cssClass;
  }

  /**
   * If true, show menu separators: `[ ' at beginning, ' | ' to separate
   * items, ' ]' at end, default true
   */
  public void setShowSeparators( boolean showSeparators )
  {
    _showSeparators = showSeparators;
  }

  protected void menuStart( StringBuffer buf )
  {
    if ( _cssClass != null ) {
      buf.append( "<div class='" );
      buf.append( _cssClass );
      buf.append( "'>" );
    }
    else {
      buf.append( "<div>");
    }
  }

  protected void menuItem( StringBuffer buf, 
                           int count, 
                           String name, 
                           String shortDescription,
                           String url,
                           boolean isSelected )

  {
    if ( _showSeparators ) {
      if ( count == 1 )
        buf.append("[ ");
      else
        buf.append(" | ");
    }

    if ( isSelected )
      buf.append("<span class='sel'");
    else {
      if ( url == null)
        buf.append("<span class='nosel'");
      else
        buf.append("<span class='unsel'");
    }

    if ( url == null && shortDescription != null ) {
      buf.append(" title='");
      appendEscaped( buf, shortDescription );
      buf.append("'");
    }

    buf.append('>');

    if ( url != null ) {
      buf.append( "<a href='" );
      buf.append( url );
      buf.append( "'");

      if ( shortDescription != null ) {
        buf.append(" title='");
        appendEscaped( buf,shortDescription );
        buf.append("'");
      }
      buf.append( ">");
    }

    appendEscaped( buf, name );

    if ( url != null )
      buf.append( "</a>" );

    buf.append( "</span>" );
  }

  protected void menuEnd( StringBuffer buf, int count )
  {
    if ( _showSeparators && count > 0 )
      buf.append("]");

    buf.append( "</div>");
  }
}
