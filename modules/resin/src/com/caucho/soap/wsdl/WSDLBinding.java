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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.xml.XmlUtil;

/**
 * WSDL binding definition
 */
public class WSDLBinding {
  private final static L10N L = new L10N(WSDLBinding.class);

  private WSDLDefinitions _defs;
  
  private QName _name;
  private WSDLPortType _portType;

  private Node _node;

  private HashMap<QName,WSDLOperation> _opMap =
    new HashMap<QName,WSDLOperation>();

  private ArrayList<Param> _params = new ArrayList<Param>();

  WSDLBinding(WSDLDefinitions defs)
  {
    _defs = defs;
  }

  /**
   * Sets the config node.
   */
  public void setConfigNode(Node node)
  {
    _node = node;
  }
  
  /**
   * Sets the binding name.
   */
  public void setName(String name)
  {
    _name = new QName(_defs.getTargetNamespace(), name);
  }

  /**
   * Returns the binding name.
   */
  public QName getName()
  {
    return _name;
  }
  
  /**
   * Sets the binding type.
   */
  public void setType(String type)
    throws ConfigException
  {
    int p = type.indexOf(':');
    String uri;

    QName qName;

    if (p < 0) {
      uri = XmlUtil.getNamespace(_node, "");
      if (uri == null)
	uri = _defs.getTargetNamespace();
      qName = new QName(uri, type);
    }
    else {
      String prefix = type.substring(0, p);
      uri = XmlUtil.getNamespace(_node, prefix);
      qName = new QName(uri, type.substring(p + 1), prefix);
    }
    
    _portType = _defs.getPortType(qName);

    if (_portType == null)
      throw new ConfigException(L.l("{0} is an unknown portType.",
				    _portType));
  }

  /**
   * Returns the port type.
   */
  public WSDLPortType getPortType()
  {
    return _portType;
  }

  /**
   * Adds an operation.
   */
  public Operation createOperation()
  {
    return new Operation();
  }

  /**
   * Adds an operation.
   */
  public void addOperation(Operation op)
  {
    _opMap.put(op.getName(), op.getOp());
  }

  /**
   * Returns the operation names.
   */
  public Iterator<QName> getOperationNames()
  {
    return _opMap.keySet().iterator();
  }

  /**
   * Returns the operation.
   */
  public WSDLOperation getOperation(QName name)
  {
    return _opMap.get(name);
  }

  /**
   * A part of the operation.
   */
  public class Operation {
    private QName _name;
    private WSDLOperation _op;

    Operation()
    {
    }

    /**
     * Sets the operation name.
     */
    public void setName(String name)
    {
      _name = new QName(_defs.getTargetNamespace(), name);

      _op = _portType.getOperation(_name);
    }

    /**
     * Returns the name.
     */
    public QName getName()
    {
      return _name;
    }

    /**
     * Returns the op.
     */
    public WSDLOperation getOp()
    {
      return _op;
    }

    /**
     * Adds an input.
     */
    public void addInput(Input input)
    {
    }

    /**
     * Adds an output.
     */
    public void addOutput(Output output)
    {
    }
  }

  /**
   * A part of the operation.
   */
  public static class Param {
  }

  /**
   * A part of the operation.
   */
  public static class Input extends Param {
  }

  /**
   * A part of the operation.
   */
  public static class Output extends Param {
  }

  /**
   * The debug string.
   */
  public String toString()
  {
    return "WSDLBinding[" + getName() + "]";
  }
}
