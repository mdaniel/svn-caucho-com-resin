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

import javax.xml.namespace.QName;

import com.caucho.util.L10N;

/**
 * WSDL Message definition
 */
public class WSDLMessage {
  private final static L10N L = new L10N(WSDLMessage.class);

  private WSDLDefinitions _defs;
  
  private QName _name;
  private ArrayList<Part> _parts = new ArrayList<Part>();

  /**
   * Creates the operation.
   */
  WSDLMessage(WSDLDefinitions defs)
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
   * Adds a message part.
   */
  public void addPart(Part part)
  {
    _parts.add(part);
  }

  /**
   * Returns the message part.
   */
  public ArrayList<Part> getParts()
  {
    return _parts;
  }

  public static class Part {
    private String _name;
    private String _type;

    /**
     * Sets the part name.
     */
    public void setName(String name)
    {
      _name = name;
    }

    /**
     * Gets the part name.
     */
    public String getName()
    {
      return _name;
    }

    /**
     * Sets the part type.
     */
    public void setType(String type)
    {
      _type = type;
    }
  }
}
