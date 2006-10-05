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
 * @author Emil Ong
 */

package com.caucho.soap.wsdl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.*;

/**
 * WSDL operation definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="operation", namespace="http://schemas.xmlsoap.org/wsdl/")
public class WSDLBindingOperation extends WSDLNamedExtensibleDocumented {
  @XmlElement(name="input", namespace="http://schemas.xmlsoap.org/wsdl/")
  private WSDLBindingOperationMessage _input;

  @XmlElement(name="output", namespace="http://schemas.xmlsoap.org/wsdl/")
  private WSDLBindingOperationMessage _output;

  @XmlElement(name="fault", namespace="http://schemas.xmlsoap.org/wsdl/")
  private List<WSDLBindingOperationFault> _faults;

  public void setInput(WSDLBindingOperationMessage input)
  {
    _input = input;
  }

  /**
   * Returns the input.
   */
  public WSDLBindingOperationMessage getInput()
  {
    return _input;
  }

  public void setOutput(WSDLBindingOperationMessage output)
  {
    _output = output;
  }

  /**
   * Returns the output.
   */
  public WSDLBindingOperationMessage getOutput()
  {
    return _output;
  }

  public void addFault(WSDLBindingOperationFault fault)
  {
    if (_faults == null)
      _faults = new ArrayList<WSDLBindingOperationFault>();

    _faults.add(fault);
  }

  public List<WSDLBindingOperationFault> getFaults()
  {
    return _faults;
  }
}
