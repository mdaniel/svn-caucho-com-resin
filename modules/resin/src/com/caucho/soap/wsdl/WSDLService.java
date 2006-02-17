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

import com.caucho.xml.XmlUtil;

/**
 * WSDL Service definition
 */
public class WSDLService {
  private final static L10N L = new L10N(WSDLService.class);

  private WSDLDefinitions _defs;

  private QName _name;

  private HashMap<QName,WSDLPort> _ports = new HashMap<QName,WSDLPort>();

  /**
   * Creates the service.
   */
  WSDLService(WSDLDefinitions defs)
  {
    _defs = defs;
  }

  /**
   * Returns the defs.
   */
  WSDLDefinitions getDefs()
  {
    return _defs;
  }
  
  /**
   * Sets the Service name.
   */
  public void setName(String name)
  {
    _name = new QName(_defs.getTargetNamespace(), name);
  }

  /**
   * Returns the Service name.
   */
  public QName getName()
  {
    return _name;
  }

  /**
   * Adds a port to the service.
   */
  public WSDLPort createPort()
  {
    return new WSDLPort(this);
  }

  /**
   * Adds a port to the service.
   */
  public void addPort(WSDLPort port)
  {
    _ports.put(port.getName(), port);
  }

  /**
   * Returns the matching port.
   */
  public WSDLPort getPort(QName name)
  {
    return _ports.get(name);
  }

  /**
   * Returns an iterator of the port names.
   */
  public Iterator<QName> getPortNames()
  {
    return _ports.keySet().iterator();
  }
}
