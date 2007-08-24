/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.regexp;

import java.util.*;

import com.caucho.quercus.env.Env;
import com.caucho.util.*;

public class Lex {
  Node _rawProg;
  Regcomp _comp;

  public void addLexeme(String pattern, int value) 
    throws IllegalRegexpException
  {
    _comp._nGroup = 0;

    Node prog = _comp.parse(new PeekString(pattern));

    _rawProg = RegOptim.appendLexeme(_rawProg, prog, value);
  }

  public Regexp compile(Env env)
  {
    return new Regexp(env, _rawProg, _comp);
  }

  public Lex(String sflags) 
  {
    int flags = 0;

    for (int i = 0; sflags != null && i < sflags.length(); i++) {
      switch (sflags.charAt(i)) {
      case 'm': flags |= Regcomp.MULTILINE; break;
      case 's': flags |= Regcomp.SINGLE_LINE; break;
      case 'i': flags |= Regcomp.IGNORE_CASE; break;
      case 'x': flags |= Regcomp.IGNORE_WS; break;
      }
    }

    _comp = new Regcomp(flags);
    _rawProg = null;
  }
}
