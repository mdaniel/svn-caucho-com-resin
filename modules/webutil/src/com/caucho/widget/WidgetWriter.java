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

import java.io.Writer;
import java.io.IOException;

import com.caucho.portal.generic.FastPrintWriter;

public class WidgetWriter
  extends FastPrintWriter
{
  private static L10N L = new L10N( WidgetWriter.class );

  static protected final Logger log = 
    Logger.getLogger( WidgetWriter.class.getName() );

  private boolean _isCompact;
  private boolean _closeElementNeeded;
  private boolean _closeElementNewline;

  public WidgetWriter( Writer writer )
    throws IOException
  {
    super( writer );
  }

  /**
   * If true do not print unnecessary newlines, default false.
   */
  public void setCompact( boolean compact )
  {
    _isCompact = compact;
  }

  /**
   * Write the '%gt;' of an element if needed, but do not flush the underlying
   * writer.
   */
  public void flush()
  {
    closeElementIfNeeded();
  }

  public void startElement( String name )
    throws IOException
  {
    startElement( name, false );
  }

  /**
   * @param name the name of the element
   * @param newline write a newline after starting the element, unless
   * compact rendering has been set.
   */
  public void startElement( String name, boolean newline )
    throws IOException
  {
    closeElementIfNeeded();

    write( '<' );
    write( name );

    _closeElementNeeded = true;
    _closeElementNewline = newline;
  }

  public void writeAttribute( String name, Object value )
    throws IOException
  {
    write(' ');
    writeEscaped( name );
    write("=\"");

    writeEscaped( value );
    write('\"');
  }

  public void writeAttribute( String name, WidgetURL url )
    throws IOException
  {
    write(' ');
    write( name );
    write("=\"");

    write( url.toString() );
    write('\"');
  }

  public void writeComment( Object comment )
    throws IOException
  {
    closeElementIfNeeded();

    write( "<!--" );
    writeEscaped( comment.toString() );
    write( "-->" );
  }

  /**
   * Write the toString() of the object, escaping as needed
   */
  public void writeText( Object object )
    throws IOException
  {
    closeElementIfNeeded();

    String string = object.toString();
    int len = string.length();

    for (int i = 0; i < len; i++) {
      writeEscaped( string.charAt(i) );
    }
  }

  /**
   * Write the char[], escaping as needed
   */
  public void writeText( char buf[] )
    throws IOException
  {
    closeElementIfNeeded();

    int endIndex = buf.length;

    for (int i = 0; i < endIndex; i++) {
      writeEscaped( buf[i] );
    }
  }

  /**
   * Write the char[], escaping as needed
   */
  public void writeText( char buf[], int offset, int length )
    throws IOException
  {
    closeElementIfNeeded();

    int endIndex = offset + length;

    for (int i = offset; i < endIndex; i++) {
      writeEscaped( buf[i] );
    }
  }

  public void writeText( char ch )
    throws IOException
  {
    closeElementIfNeeded();

    writeEscaped( ch );
  }

  public void endElement( String name )
    throws IOException
  {
    endElement( name, false );
  }

  /**
   * @param name the name of the element
   * @param newline write a newline after closing the element, unless
   * compact rendering has been set
   */
  public void endElement( String name, boolean newline )
    throws IOException
  {
    closeElementIfNeeded();

    write( "</" );
    write( name );
    write( ">" );

    if ( newline )
      printlnUnlessCompact();
  }

  public void close()
  {
    closeElementIfNeeded();

    super.close();
  }

  protected void closeElementIfNeeded()
  {
    if ( _closeElementNeeded ) {
      _closeElementNeeded = false;
      write( '>' );

      if ( _closeElementNewline ) {
        printlnUnlessCompact();
        _closeElementNewline = false;
      }
    }
  }

  private void printlnUnlessCompact()
  {
    if ( ! _isCompact )
      println();
  }

  private void writeEscaped( Object object )
    throws IOException
  {
    String string = object.toString();

    int len = string.length();

    for (int i = 0; i < len; i++) {
      writeEscaped( string.charAt(i) );
    }
  }

  private void writeEscaped( char ch ) 
    throws IOException
  {
    switch ( ch ) {
      case '<': write( "&lt;" ); break;
      case '>': write( "&gt;" ); break;
      case '&': write( "&amp;" ); break;
      case '\"': write( "&quot;" ); break;
      case '\'': write( "&rsquo;" ); break;
      default: write( ch );
    }
  }
}
