/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.servlets.ssi;

import com.caucho.vfs.Path;

/**
 * Parses an SSI expression
 */
public class ExprParser {
  private String _expr;
  private int _index;
  private Path _path;

  private StringBuilder _sb = new StringBuilder();

  private ExprParser(String expr, Path path)
  {
    _expr = expr;
    _path = path;
  }
  
  /**
   * parse a string.
   */
  public static SSIExpr parseString(String expr, Path path)
  {
    return new ExprParser(expr, path).parseString();
  }

  private SSIExpr parseString()
  {
    return parseTerm();
  }
  
  private SSIExpr parseTerm()
  {
    int ch;

    SSIExpr expr = null;
    
    while ((ch = read()) >= 0) {
      if (ch == '$') {
	if (_sb.length() > 0)
	  expr = ConcatExpr.create(expr, new StringExpr(_sb.toString()));
	_sb.setLength(0);

	ch = read();
	if (ch == '{') {
	  for (ch = read(); ch >= 0 && ch != '}'; ch = read()) {
	    _sb.append((char) ch);
	  }
	}
	else {
	  for (;
	       'a' <= ch && ch <= 'z'
                 || 'A' <= ch && ch <= 'Z'
                 || '0' <= ch && ch <= '9'
                 || ch == '_';
	       ch = read()) {
	    _sb.append((char) ch);
	  }

	  unread();
	}

	expr = ConcatExpr.create(expr, new VarExpr(_sb.toString(), _path));
	_sb.setLength(0);
      }
      else if (ch == '\\') {
	ch = read();

	if (ch == '$')
	  _sb.append((char) ch);
	else if (ch == '\\')
	  _sb.append((char) ch);
	else {
	  _sb.append('\\');
	  unread();
	}
      }
      else
	_sb.append((char) ch);
    }

    if (_sb.length() > 0)
      expr = ConcatExpr.create(expr, new StringExpr(_sb.toString()));

    return expr;
  }

  private int read()
  {
    if (_index < _expr.length())
      return _expr.charAt(_index++);
    else {
      _index++;
      return -1;
    }
  }

  private void unread()
  {
    _index--;
  }
}
