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

package com.caucho.v5.config.type;

import java.util.function.Function;

import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;

public class PathType extends ConfigType<PathImpl>
{
  private static final L10N L = new L10N(PathType.class);

  public static final PathType TYPE = new PathType();

  private PathType()
  {
  }
  

  /**
   * Returns the path class.
   */
  @Override
  public Class<PathImpl> getType()
  {
    return PathImpl.class;
  }

  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  @Override
  public Object valueOf(String text)
  {
    return lookupPath(text);
  }
  
  /**
   * Converts the value to a value of the type.
   */
  @Override
  public Object valueOf(Object value)
  {
    if (value instanceof PathImpl)
      return value;
    if (value instanceof String)
      return valueOf((String) value);
    else if (value == null)
      return null;
    else
      return valueOf(String.valueOf(value));
  }

  public static PathImpl lookupPath(String string)
  {
    return lookupPath(string,
                      Vfs.lookup());
  }

  public static PathImpl lookupPath(String string, PathImpl pwd)
  {
    string = rewritePathString(string);

    //Expr expr = new ELParser(env, string).parse();
    Function<String,Object> env = x->null;
    Object obj = ExprCfg.newParser(string).parse().eval(env);

    //Object obj = expr.evalObject(env);

    if (obj == null) {
      throw new NullPointerException(L.l("Path '{0}' evaluated to null.",
                                         string));
    }
    
    if (obj instanceof PathImpl) {
      return (PathImpl) obj;
    }

    //String value = Expr.toString(obj, env);
    String value = String.valueOf(obj);

    if (pwd != null) {
      return pwd.lookup(value);
    }
    else {
      return Vfs.lookup(value);
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
    if (pathName == null)
      return ".";
    
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

        cb.append("${" + pathName.substring(i + 1, tail) + "}");
        i = tail - 1;
      }
      else
        cb.append('$');
    }

    return cb.toString();
  }
}
