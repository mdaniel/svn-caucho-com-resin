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

import javax.xml.namespace.QName;

import com.caucho.util.L10N;

/**
 * WSDL Definitions top level
 */
public class WSDLDefinitions {
  private final static L10N L = new L10N(WSDLParser.class);

  private String _name;
  private String _targetNamespace;

  private HashMap<QName,WSDLMessage> _messageMap =
    new HashMap<QName,WSDLMessage>();

  private HashMap<QName,WSDLPortType> _portTypeMap =
    new HashMap<QName,WSDLPortType>();

  private HashMap<QName,WSDLBinding> _bindingMap =
    new HashMap<QName,WSDLBinding>();

  private HashMap<QName,WSDLService> _serviceMap =
    new HashMap<QName,WSDLService>();

  /**
   * Sets the definition name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the target namespace
   */
  public void setTargetNamespace(String uri)
  {
    _targetNamespace = uri;
  }

  /**
   * Gets the target namespace
   */
  public String getTargetNamespace()
  {
    return _targetNamespace;
  }

  /**
   * Create a message.
   */
  public WSDLMessage createMessage()
  {
    return new WSDLMessage(this);
  }

  /**
   * Adds a message.
   */
  public void addMessage(WSDLMessage message)
  {
    _messageMap.put(message.getName(), message);
  }

  /**
   * Gest a message.
   */
  public WSDLMessage getMessage(QName name)
  {
    return _messageMap.get(name);
  }

  /**
   * Creates a PortType.
   */
  public WSDLPortType createPortType()
  {
    return new WSDLPortType(this);
  }

  /**
   * Adds a PortType.
   */
  public void addPortType(WSDLPortType portType)
  {
    _portTypeMap.put(portType.getName(), portType);
  }

  /**
   * Gets a PortType.
   */
  public WSDLPortType getPortType(QName name)
  {
    return _portTypeMap.get(name);
  }

  /**
   * Adds a Binding
   */
  public WSDLBinding createBinding()
  {
    return new WSDLBinding(this);
  }

  /**
   * Adds a Binding
   */
  public void addBinding(WSDLBinding binding)
  {
    _bindingMap.put(binding.getName(), binding);
  }

  /**
   * Gets the matching Binding
   */
  public WSDLBinding getBinding(QName name)
  {
    return _bindingMap.get(name);
  }

  /**
   * Adds a Service
   */
  public WSDLService createService()
  {
    return new WSDLService(this);
  }

  /**
   * Adds a Service
   */
  public void addService(WSDLService service)
  {
    _serviceMap.put(service.getName(), service);
  }

  /**
   * Gets a service.
   */
  public WSDLService getService(QName name)
  {
    return _serviceMap.get(name);
  }
}
