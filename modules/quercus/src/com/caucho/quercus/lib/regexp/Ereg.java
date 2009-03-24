/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.regexp;

import java.util.logging.*;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.*;

public class Ereg extends Regexp
{
  private static final Logger log
    = Logger.getLogger(Ereg.class.getName());

  private static final L10N L = new L10N(Ereg.class);
  
  public Ereg(StringValue rawRegexp)
    throws IllegalRegexpException
  {
    super(rawRegexp);
  }
  
  @Override
  protected void init()
  {
    StringValue rawRegexp = _rawRegexp;
    
    if (rawRegexp.length() < 2) {
      throw new IllegalStateException(L.l(
          "Can't find delimiters in regexp '{0}'.",
          rawRegexp));
    }

    int head = 0;
    
    char delim = '/';

    for (;
     head < rawRegexp.length()
       && Character.isWhitespace((delim = rawRegexp.charAt(head)));
     head++) {
    }

    if (delim == '{')
      delim = '}';
    else if (delim == '[')
      delim = ']';
    else if (delim == '(')
      delim = ')';
    else if (delim == '<')
      delim = '>';
    else if (delim == '\\' || Character.isLetterOrDigit(delim)) {
      throw new QuercusException(L.l(
          "Delimiter {0} in regexp '{1}' must not be backslash or alphanumeric.",
          String.valueOf(delim),
          rawRegexp));
    }

    int tail = rawRegexp.lastIndexOf(delim);

    if (tail <= 0)
      throw new QuercusException(L.l(
          "Can't find second {0} in regexp '{1}'.",
          String.valueOf(delim),
          rawRegexp));

    StringValue sflags = rawRegexp.substring(tail);
    StringValue pattern = rawRegexp.substring(head + 1, tail); 
    
    int flags = 0;
    
    for (int i = 0; sflags != null && i < sflags.length(); i++) {
      switch (sflags.charAt(i)) {
        case 'm': flags |= Regcomp.MULTILINE; break;
        case 's': flags |= Regcomp.SINGLE_LINE; break;
        case 'i': flags |= Regcomp.IGNORE_CASE; break;
        case 'x': flags |= Regcomp.IGNORE_WS; break;
        case 'g': flags |= Regcomp.GLOBAL; break;
        
        case 'A': flags |= Regcomp.ANCHORED; break;
        case 'D': flags |= Regcomp.END_ONLY; break;
        case 'U': flags |= Regcomp.UNGREEDY; break;
        case 'X': flags |= Regcomp.STRICT; break;
        
        case 'u': flags |= Regcomp.UTF8; break;
        case 'e': _isEval = true; break;
      }
    }

    // XXX: what if unicode.semantics='true'?
    
    if ((flags & Regcomp.UTF8) != 0) {
      _pattern = fromUtf8(pattern);
      
      if (pattern == null)
        throw new QuercusException(L.l("Regexp: error converting subject to utf8"));
    }
  }
  
  public String toString()
  {
    return "Ereg[" + _pattern + "]";
  }
}
