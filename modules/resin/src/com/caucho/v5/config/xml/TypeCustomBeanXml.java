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
 * @author Scott Ferguson
 */

package com.caucho.v5.config.xml;

import java.util.logging.Logger;

import org.w3c.dom.Node;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.custom.TypeCustomBean;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.util.L10N;

/**
 * Represents a CanDI-style introspected bean type for configuration.
 */
public class TypeCustomBeanXml<T> extends TypeCustomBean<T>
{
  private static final L10N L = new L10N(TypeCustomBeanXml.class);
  private static final Logger log
    = Logger.getLogger(TypeCustomBeanXml.class.getName());

  private static final String RESIN_NS
    = "http://caucho.com/ns/resin";
  private static final String JAVAEE_NS
    = "http://java.sun.com/xml/ns/javaee";

  private static final NameCfg TEXT = new NameCfg("#text");

  private static final NameCfg W_VALUE = new NameCfg("", "value", JAVAEE_NS);
  private static final NameCfg R_VALUE = new NameCfg("", "value", RESIN_NS);
  private static final NameCfg A_VALUE = new NameCfg("value", null);

  private static final NameCfg W_NEW = new NameCfg("", "new", JAVAEE_NS);
  private static final NameCfg R_NEW = new NameCfg("", "new", RESIN_NS);
  private static final NameCfg A_NEW = new NameCfg("new", null);

  public TypeCustomBeanXml(Class<T> beanClass, ConfigType<T> type)
  {
    super(beanClass, type);

    /*
    _nsAttributeMap.put(W_NEW, XmlBeanNewAttribute.ATTRIBUTE);
    _nsAttributeMap.put(R_NEW, XmlBeanNewAttribute.ATTRIBUTE);
    _nsAttributeMap.put(A_NEW, XmlBeanNewAttribute.ATTRIBUTE);
    */
  }

  /**
   * Called before the children are configured.  Also called for
   * attribute configuration, e.g. for macros and web-app-default.
   */
  @Override
  public void beforeConfigure(ContextConfig builder, Object bean, Node node)
  {
    super.beforeConfigure(builder, bean, node);
    
    /*
    if (bean instanceof ConfigCustomBean) {
      ConfigCustomBean xmlBean = (ConfigCustomBean) bean;
      
      if (node instanceof QNode) {
        QNode qNode = (QNode) node;

        String uri = qNode.getBaseURI();
        int line = qNode.getLine();
        
        xmlBean.setConfigLocation(uri, line);
      }
    }
    */
  }
}
