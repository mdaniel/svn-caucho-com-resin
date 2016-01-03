/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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
 * @author Scott Ferguson;
 */

package com.caucho.v5.config.xml;

import javax.enterprise.context.spi.CreationalContext;

import org.w3c.dom.Node;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.program.FlowProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;
import com.caucho.v5.xml.QNode;

/**
 * Stored configuration program for an attribute.
 */
public class ProgramNodeChild extends FlowProgram {
  static final L10N L = new L10N(ProgramNodeChild.class);

  private final Node _node;

  public ProgramNodeChild(ConfigXml config,
                          Node node)
  {
    super(config);
    
    _node = node;
  }
  
  @Override
  protected ConfigXml getConfig()
  {
    return (ConfigXml) super.getConfig();
  }

  @Override
  public NameCfg getQName()
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
  public <T> void inject(T bean, InjectContext cxt)
    throws ConfigException
  {
    ContextConfigXml env = getConfig().currentOrCreateContext();
    
    InjectContext oldCxt = env.setCreationalContext(cxt);
    
    try {
      env.configureAttribute(bean, _node);
    } finally {
      env.setCreationalContext(oldCxt);
    }
  }
  
  @Override
  public ContainerProgram toContainer()
  {
    ContainerProgram program = new ContainerProgram();
    
    for (Node node = _node.getFirstChild();
        node != null;
        node = node.getNextSibling()) {
      program.addProgram(new ProgramNodeChild(getConfig(), node));
    }
    
    return program;
  }

  @Override
  public <T> T create(ConfigType<T> type, InjectContext cxt)
    throws ConfigException
  {
    ContextConfigXml env = getConfig().currentOrCreateContext();
    
    return (T) env.create(_node, type);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _node + "]";
  }
}
