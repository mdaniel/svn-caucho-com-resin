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

import org.w3c.dom.Node;

import com.caucho.v5.config.Config;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.inject.InjectContext;
import com.caucho.v5.config.program.FlowProgram;
import com.caucho.v5.util.L10N;
import com.caucho.v5.xml.QElement;
import com.caucho.v5.xml.QNode;

/**
 * Stored configuration program for an attribute.
 */
public class ProgramNode extends FlowProgram {
  static final L10N L = new L10N(ProgramNode.class);

  public static final ProgramNode NULL
    = new ProgramNode(Config.getDefaultConfig(), new QElement());

  private final Node _node;

  private ProgramNode(Config config,
                      Node node)
  {
    super(config);
    
    _node = node;
  }

  @Override
  public NameCfg getQName()
  {
    if (_node instanceof QNode)
      return ((QNode) _node).getQName();
    else
      return null;
  }

  @Override
  public <T> void inject(T bean, InjectContext cxt)
    throws ConfigException
  {
    ContextConfigXml env = ContextConfigXml.getCurrent();
    
    env.configureBean(bean, _node);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _node + "]";
  }
}
