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
import com.caucho.util.CharBuffer;

import java.util.logging.Level;
import java.util.logging.Logger;


public class WidgetUtil
{
  private static L10N L = new L10N( WidgetUtil.class );

  static protected final Logger log = 
    Logger.getLogger( WidgetUtil.class.getName() );

  /**
   * Calculate an id based on the hashCode and possibly the value of toString()
   * of an Object, the calculated id is suitable as a java identifier.
   *
   * @param len the number of characters in the returned string, greater than
   * 8 and o.toString() is used, maximum of 24
   */
  static public String calculateId( Object o, int len )
  {
    if ( len > 24 )
      throw new IllegalArgumentException("length exceeded");

    int len1 = len > 8 ? 8 : len;

    CharBuffer cid = CharBuffer.allocate();

    int hashCode = o.hashCode();

    for ( int i = 0; i < len1; i++ ) {
      int c = (hashCode >> (i * 4)) & 0x3f;

      if (c < 26)
        cid.append( (char) (c + 'A') );
      else if (c < 52)
        cid.append( (char) (c + 'a' - 26) );
      else {
        if ( i == 0 ) {
          cid.append( '_' );
        }
        else {
          if (c < 62)
            cid.append( (char) (c + '0' - 52) );
          else
            cid.append( '_' );
        }
      }
    }

    if ( len  > len1 ) {
      len1 = len - len1;

      String toString = o.toString();
      long longCode = 0;

      if ( toString == null )
        longCode = 26010331;
      else
        for ( int i = 0; i < toString.length(); i++ ) {
          longCode = 331 * longCode + toString.charAt( i );
        }

      for ( int i = 0; i < len1; i++ ) {
        long c =  (longCode >> (i * 4)) & 0x3f;

        if (c < 26)
          cid.append( (char) (c + 'A') );
        else if (c < 52)
          cid.append( (char) (c + 'a' - 26) );
        else if (c < 62)
          cid.append( (char) (c + '0' - 52) );
        else
          cid.append( '_' );
      }
    }

    return cid.close();
  }
}
