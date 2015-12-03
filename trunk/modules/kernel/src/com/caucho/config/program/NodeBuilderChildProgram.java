/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import javax.enterprise.context.spi.CreationalContext;

import org.w3c.dom.Node;

import com.caucho.config.ConfigException;
import com.caucho.config.type.ConfigType;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.util.L10N;
import com.caucho.xml.QName;
import com.caucho.xml.QNode;

/**
 * Stored configuration program for an attribute.
 */
public class NodeBuilderChildProgram extends FlowProgram {
  static final L10N L = new L10N(NodeBuilderChildProgram.class);

  private final Node _node;

  public NodeBuilderChildProgram(Node node)
  {
    if (node == null)
      throw new NullPointerException();
    
    _node = node;
  }

  @Override
  public QName getQName()
  {
    if (_node instanceof QNode)
      return ((QNode) _node).getQName();
    else
      return null;
  }

  public Node getNode()
  {
    return _node;
  }

  @Override
  public <T> void inject(T bean, CreationalContext<T> cxt)
    throws ConfigException
  {
    XmlConfigContext env = XmlConfigContext.create();
    
    CreationalContext<?> oldCxt = env.setCreationalContext(cxt);
    
    try {
      env.configureAttribute(bean, _node);
    } finally {
      env.setCreationalContext(oldCxt);
    }
  }

  @Override
  public <T> T create(ConfigType<T> type, CreationalContext<T> cxt)
    throws ConfigException
  {
    XmlConfigContext env = XmlConfigContext.create();
    
    return (T) env.create(_node, type);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _node + "]";
  }
}
