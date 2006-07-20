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

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.xml.XmlUtil;

/**
 * WSDL operation definition
 */
public class WSDLOperation {
  private final static L10N L = new L10N(WSDLOperation.class);

  private WSDLDefinitions _defs;
  
  private QName _name;

  private ArrayList<Param> _params = new ArrayList<Param>();

  /**
   * Creates the operation.
   */
  WSDLOperation(WSDLDefinitions defs)
  {
    _defs = defs;
  }

  /**
   * Creates the operation.
   */
  WSDLOperation(WSDLOperation op)
  {
    _defs = op._defs;
    _name = op._name;

    for (int i = 0; i < op._params.size(); i++) {
      Param param = op._params.get(i);

      _params.add(param);
    }
  }

  /**
   * Returns the defs.
   */
  WSDLDefinitions getDefs()
  {
    return _defs;
  }
  
  /**
   * Sets the operation name.
   */
  public void setName(String name)
  {
    _name = new QName(getDefs().getTargetNamespace(), name);
  }
  
  /**
   * Sets the operation name.
   */
  public void setName(QName name)
  {
    _name = name;
  }

  /**
   * Returns the operation name.
   */
  public QName getName()
  {
    return _name;
  }

  /**
   * Creates the input.
   */
  public Input createInput()
  {
    Input input = new Input();
    
    _params.add(input);

    return input;
  }

  /**
   * Adds the output.
   */
  public void addOutput(Output output)
  {
    _params.add(output);
  }

  /**
   * Returns the input.
   */
  public WSDLMessage getInput()
  {
    for (int i = 0; i < _params.size(); i++) {
      Param param = _params.get(i);

      if (param instanceof Input) {
        return param.getMessage();
      }
    }

    return null;
  }

  /**
   * A part of the operation.
   */
  public class Param {
    private Node _node;
  
    private WSDLMessage _message;
    
    /**
     * Sets the configuration node (for the namespace).
     */
    public void setConfigNode(Node node)
    {
      _node = node;
    }
    
    /**
     * Sets the message definition for the param.
     */
    public void setMessage(String name)
      throws ConfigException
    {
      int p = name.indexOf(':');
      String uri;

      QName messageName;

      if (p < 0) {
        uri = XmlUtil.getNamespace(_node, "");
        messageName = new QName(uri, name);
      }
      else {
        String prefix = name.substring(0, p);
        uri = XmlUtil.getNamespace(_node, prefix);
        messageName = new QName(uri, name.substring(p + 1), prefix);
      }

      _message = _defs.getMessage(messageName);

      if (_message == null)
        throw new ConfigException(L.l("{0} is an unknown message.",
                                      messageName));
    }

    public WSDLMessage getMessage()
    {
      return _message;
    }
  }

  /**
   * A part of the operation.
   */
  public class Input extends Param {
  }

  /**
   * A part of the operation.
   */
  public class Output extends Param {
  }
}
