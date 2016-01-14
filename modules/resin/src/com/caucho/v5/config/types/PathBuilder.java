/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Special builder for path variables.
 */
public class PathBuilder {
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
  public PathImpl replaceObject()
  {
    return lookupPath(_userPath, ConfigContext.getEnvironment());
  }

  public static PathImpl lookupPath(String string)
  {
    return lookupPath(string, ConfigContext.getEnvironment());
  }

  public static PathImpl lookupPath(String string, ArrayList<?> vars)
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);
    
    return lookupPath(string, map);
  }

  public static PathImpl lookupPath(String string, Map<String,Object> map)

  {
    /*
    ELContext context;

    if (map != null) {
      ELResolver resolver = new MapVariableResolver(map);
      context = new ConfigELContext(resolver);
    }
    else
      context = new ConfigELContext();

    return lookupPath(string, context);
    */
    if (map != null) {
      return lookupPath(string, x->map.get(x));
    }
    else {
      return lookupPath(string, x->ConfigContext.getProperty(x));
    }
  }

  public static PathImpl lookupPath(String string, Function<String,Object> env)
  {
    return lookupPath(string, env, VfsOld.lookup());
  }

  public static PathImpl lookupPath(String string, Function<String,Object> env, PathImpl pwd)
  {
    if (env == null) {
      env = ConfigContext.getEnvironment();
    }
    
    string = rewritePathString(string);

    // Expr expr = new ELParser(env, string).parse();

    Object obj = ExprCfg.newParser(string).parse().eval(env);

    if (obj instanceof PathImpl) {
      return (PathImpl) obj;
    }

    //String value = Expr.toString(obj, env);
    String value = String.valueOf(obj);

    if (pwd != null) {
      return pwd.lookup(value);
    }
    else {
      return VfsOld.lookup(value);
    }
  }
  
  /**
   * Rewrites the path string into proper JSP EL.
   *
   * Returns the native path for a configured path name.
   *
   * @return a real path corresponding to the path name
   */
  public static String rewritePathString(String pathName)
  {
    StringBuilder cb = new StringBuilder();

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
        // cb.append("${Var[\"" + pathName.substring(i + 1, tail) + "\"]}");
        cb.append("${" + pathName.substring(i + 1, tail) + "}");
        i = tail - 1;
      }
      else
        cb.append('$');
    }
    
    return cb.toString();
  }
}
