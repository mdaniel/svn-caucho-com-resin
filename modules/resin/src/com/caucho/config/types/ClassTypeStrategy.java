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

import com.caucho.config.NodeBuilder;
import com.caucho.config.TypeStrategy;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import java.util.HashMap;

import java.lang.reflect.Array;

public class ClassTypeStrategy extends TypeStrategy {
  protected static final L10N L = new L10N(ClassTypeStrategy.class);

  private static final HashMap<String,Class> _primitiveTypes =
    new HashMap<String,Class>();


    /**
     * Configures the node as a string object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
  public Object configure(NodeBuilder builder, Node node, Object parent)
    throws Exception
  {
    String className = builder.configureString(node);

    Class cl = _primitiveTypes.get(className);

    if (cl != null)
      return cl;
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    if (className != null && ! className.equals("")) { 
      int parIdx = className.indexOf('[');

      if (parIdx > 0) {
        String cn = className.substring(0, parIdx);
        cl = _primitiveTypes.get(cn);

        if (cl == null) 
          cl = Class.forName(cn, false, loader);

        while (parIdx > 0) {
          cl = Array.newInstance(cl, 0).getClass();
          parIdx = className.indexOf('[', (parIdx + 1));
        }

        return cl;
      } else {

        return Class.forName(className, false, loader);
      }
    } else {

      return null;
    }
  }

  static {
    _primitiveTypes.put("void", void.class);
    _primitiveTypes.put("boolean", boolean.class);
    _primitiveTypes.put("char", char.class);
    _primitiveTypes.put("byte", byte.class);
    _primitiveTypes.put("short", short.class);
    _primitiveTypes.put("int", int.class);
    _primitiveTypes.put("long", long.class);
    _primitiveTypes.put("float", float.class);
    _primitiveTypes.put("double", double.class);

    _primitiveTypes.put("Boolean", Boolean.class);
    _primitiveTypes.put("Character", Character.class);
    _primitiveTypes.put("Byte", Byte.class);
    _primitiveTypes.put("Short", Short.class);
    _primitiveTypes.put("Integer", Integer.class);
    _primitiveTypes.put("Long", Long.class);
    _primitiveTypes.put("Float", Float.class);
    _primitiveTypes.put("Double", Double.class);
    _primitiveTypes.put("String", String.class);
    _primitiveTypes.put("Date", java.util.Date.class);
  }
}
