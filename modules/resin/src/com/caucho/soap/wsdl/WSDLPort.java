/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.soap.wsdl;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.xml.XmlUtil;

/**
 * WSDL Port definition
 */
public class WSDLPort {
  private final static L10N L = new L10N(WSDLPort.class);

  private WSDLService _service;
  
  private Node _node;
  
  private QName _name;
  
  private WSDLBinding _binding;

  private HashMap<QName,WSDLOperation> _opMap =
    new HashMap<QName,WSDLOperation>();

  /**
   * Creates the port.
   */
  WSDLPort(WSDLService service)
  {
    _service = service;
  }

  /**
   * Returns the defs.
   */
  WSDLDefinitions getDefs()
  {
    return _service.getDefs();
  }
  
  /**
   * Sets the Service name.
   */
  public void setName(String name)
  {
    _name = new QName(_service.getDefs().getTargetNamespace(), name);
  }

  /**
   * Returns the Service name.
   */
  public QName getName()
  {
    return _name;
  }
    
  /**
   * Sets the configuration node (for the namespace).
   */
  public void setConfigNode(Node node)
  {
    _node = node;
  }
    
  /**
   * Sets the binding name.
   */
  public void setBinding(String name)
    throws ConfigException
  {
    int p = name.indexOf(':');
    String uri;

    QName bindingName;

    if (p < 0) {
      uri = XmlUtil.getNamespace(_node, "");
      bindingName = new QName(uri, name);
    }
    else {
      String prefix = name.substring(0, p);
      uri = XmlUtil.getNamespace(_node, prefix);
      bindingName = new QName(uri, name.substring(p + 1), prefix);
    }

    _binding = _service.getDefs().getBinding(bindingName);

    if (_binding == null)
      throw new ConfigException(L.l("{0} is an unknown binding.",
				    bindingName));
  }

  /**
   * Gets the operation.
   */
  public WSDLOperation getOperation(QName name)
  {
    return _opMap.get(name);
  }

  /**
   * Initialize the port.
   */
  public void init()
  {
    WSDLPortType portType = _binding.getPortType();
    
    Iterator<QName> names = _binding.getOperationNames();

    while (names.hasNext()) {
      QName opName = names.next();

      WSDLOperation op = _opMap.get(opName);
      if (op != null)
	continue;

      WSDLOperation oldOp = portType.getOperation(opName);
      if (oldOp == null)
	throw new NullPointerException("eep");

      op = new WSDLOperation(oldOp);

      _opMap.put(op.getName(), op);
    }
  }
}
