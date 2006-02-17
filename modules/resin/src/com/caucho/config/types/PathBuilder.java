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
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import java.util.*;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import com.caucho.el.MapVariableResolver;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.el.EL;

import com.caucho.config.BeanBuilderException;
import com.caucho.config.Config;

/**
 * Special builder for path variables.
 */
public class PathBuilder {
  private static final L10N L = new L10N(PathBuilder.class);

  private String _userPath;

  /**
   * Sets the text.
   */
  public void addText(RawString text)
  {
    _userPath = text.getValue().trim();
  }

  /**
   * Replace with the real path.
   */
  public Path replaceObject()
    throws ELException
  {
    return lookupPath(_userPath, Config.getEnvironment());
  }

  public static Path lookupPath(String string)
    throws ELException
  {
    return lookupPath(string, Config.getEnvironment());
  }

  public static Path lookupPath(String string, ArrayList vars)
    throws ELException
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);
    
    return lookupPath(string, map);
  }

  public static Path lookupPath(String string, Map<String,Object> map)
    throws ELException
  {
    VariableResolver parent = Config.getEnvironment();
    VariableResolver resolver;

    if (map != null)
      resolver = new MapVariableResolver(map, parent);
    else
      resolver = parent;
    
    return lookupPath(string, resolver);
  }

  public static Path lookupPath(String string, VariableResolver env)
    throws ELException
  {
    return lookupPath(string, env, Vfs.lookup());
  }

  public static Path lookupPath(String string, VariableResolver env, Path pwd)
    throws ELException
  {
    if (env == null)
      env = Config.getEnvironment();
    
    string = rewritePathString(string);

    Expr expr = new ELParser(string).parse();

    Object obj = expr.evalObject(env);

    if (obj instanceof Path)
      return (Path) obj;

    String value = Expr.toString(obj, env);
    
    if (pwd != null)
      return pwd.lookup(value);
    else
      return Vfs.lookup(value);
  }
  
  /**
   * Rewrites the path string into proper JSP EL.
   *
   * Returns the native path for a configured path name.  The special cases
   * $app-dir and $resin-home specify the root directory.
   *
   * @return a real path corresponding to the path name
   */
  public static String rewritePathString(String pathName)
  {
    CharBuffer cb = CharBuffer.allocate();

    int length = pathName.length();
    for (int i = 0; i < length; i++) {
      char ch = pathName.charAt(i);

      if (ch != '$') {
        cb.append(ch);
        continue;
      }
      
      if (i + 1 == length) {
        cb.append('$');
        continue;
      }

      ch = pathName.charAt(i + 1);
      
      if ('0' <= ch && ch <= '9') {
        int value = 0;

        for (i++;
             i < length && (ch = pathName.charAt(i)) >= '0' && ch <= '9';
             i++) {
          value = 10 * value + ch - '0';
        }

        i--;

        cb.append("${regexp[" + value + "]}");
      }
      else if ('a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z') {
        int tail = i + 1;
        for (; tail < length; tail++) {
          ch = pathName.charAt(tail);
        
          if (ch == '/' || ch == '\\' || ch == '$')
            break;
        }

        cb.append("${Var[\"" + pathName.substring(i + 1, tail) + "\"]}");
        i = tail - 1;
      }
      else
        cb.append('$');
    }
    
    return cb.close();
  }
}
