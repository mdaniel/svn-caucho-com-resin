/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Adam Megacz
 */

package com.caucho.xml.stream;

import java.io.*;
import java.util.logging.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

public class Escapifier {

  public static String escape(String s)
  {
    if (s == null)
      return "";
    
    CharBuffer cb = null;
    int len = s.length();

    for(int i=0; i<len; i++) {
      char c = s.charAt(i);

      if (c >= 32 && c <= 127 && c!='&' && c!='<' &&  c!='\"' // && c!='\'' 
          || Character.isWhitespace(c)) {
        if (cb != null) cb.append(c);
        continue;
      }
      if (cb == null) {
        cb = new CharBuffer();
        cb.append(s.substring(0, i));
      }
      switch(c) {
      case '&': cb.append("&amp;"); break;
      case '<': cb.append("&lt;"); break;
      // case '\'': cb.append("&apos;"); break; // TCK compliance
      case '\"': cb.append("&quot;"); break;
      default: cb.append("&#"+((int)(c & 0xffff))+";"); break;
      }
    }

    if (cb == null)
      return s;

    return cb.toString();
  }

  public static void escape(String s, WriteStream ws)
    throws IOException
  {
    ws.print(escape(s));
  }

  public static void escape(char[] c, int start, int len, WriteStream ws)
    throws IOException
  {
    ws.print(escape(new String(c, start, len)));
  }

}
