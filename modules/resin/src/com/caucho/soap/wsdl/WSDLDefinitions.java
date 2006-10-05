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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.*;

/**
 * WSDL Definitions top level
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="definitions", 
                namespace="http://schemas.xmlsoap.org/wsdl/")
public class WSDLDefinitions extends WSDLExtensibleDocumented {
  @XmlAttribute(name="name", namespace="http://schemas.xmlsoap.org/wsdl/")
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  private String _name;

  @XmlAttribute(name="targetNamespace", 
                namespace="http://schemas.xmlsoap.org/wsdl/")
  private String _targetNamespace;

  @XmlElements({
    @XmlElement(name="portType", namespace="http://schemas.xmlsoap.org/wsdl/", 
                required=true, type=WSDLPortType.class),
    @XmlElement(name="types", namespace="http://schemas.xmlsoap.org/wsdl/", 
                required=true, type=WSDLTypes.class),
    @XmlElement(name="message", namespace="http://schemas.xmlsoap.org/wsdl/", 
                required=true, type=WSDLMessage.class),
    @XmlElement(name="import", namespace="http://schemas.xmlsoap.org/wsdl/", 
                required=true, type=WSDLImport.class),
    @XmlElement(name="service", namespace="http://schemas.xmlsoap.org/wsdl/", 
                required=true, type=WSDLService.class),
    @XmlElement(name="binding", namespace="http://schemas.xmlsoap.org/wsdl/", 
                required=true, type=WSDLBinding.class)
  })
  private List<WSDLDefinition> _definitions;

  /**
   * Sets the definition name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
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

  public List<WSDLDefinition> getDefinitions()
  {
    return _definitions;
  }

  public void addDefinition(WSDLDefinition definition)
  {
    if (_definitions == null)
      _definitions = new ArrayList<WSDLDefinition>();

    _definitions.add(definition);
  }
}
