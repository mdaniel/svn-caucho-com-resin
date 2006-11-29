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
 * afloat with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.jaxb;

import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StringProperty extends JaxbProperty {
  private final Method _method;
  
  public StringProperty(Method method)
  {
    _method = method;
  }
 
  /**
   * Configures the parent object with the given node.
   *
   * @param builder the calling node builder (context)
   * @param bean the bean to be configured
   * @param name the name of the property
   * @param node the configuration node for the value
   */
  public void configureAttribute(NodeBuilder builder,
				 Object bean,
				 QName name,
				 String value)
    throws ConfigException
  {
  }
 
  /**
   * Configures the parent object with the given node.
   *
   * @param builder the calling node builder (context)
   * @param bean the bean to be configured
   * @param name the name of the property
   * @param node the configuration node for the value
   */
  public void configureElement(NodeBuilder builder,
			       Object bean,
			       QName name,
			       Node node)
    throws ConfigException
  {
    String textValue = node.getTextContent().trim();

    String value = builder.evalString(textValue);

    try {
      _method.invoke(bean, value);
    } catch (IllegalAccessException e) {
      throw builder.error(e, node);
    } catch (InvocationTargetException e) {
      throw builder.error(e.getCause(), node);
    }
  }
}
