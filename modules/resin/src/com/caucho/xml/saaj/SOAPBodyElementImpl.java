/*
* Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
*
*   Free Software Foundation, Inc.
*   59 Temple Place, Suite 330
*   Boston, MA 02111-1307  USA
*
* @author Emil Ong
*/

package com.caucho.xml.saaj;

import javax.xml.XMLConstants;
import javax.xml.namespace.*;
import javax.xml.soap.*;

import org.w3c.dom.*;

import com.caucho.xml.QNode;

public class SOAPBodyElementImpl extends SOAPElementImpl
                                 implements SOAPBodyElement 
{
  SOAPBodyElementImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);
  }

  SOAPBodyElementImpl(SOAPFactory factory, SOAPElement element)
    throws SOAPException
  {
    this(factory, NameImpl.fromQName(element.getElementQName()));

    copySOAPElement(element);
  }

  SOAPBodyElementImpl(SOAPFactory factory, Element element)
    throws SOAPException
  {
    this(factory, NameImpl.fromElement(element));

    copyElement(element, true);
  }

  public void setParentElement(SOAPElement parent)
    throws SOAPException
  {
    if (parent == null)
      throw new IllegalArgumentException();

    if ((parent instanceof SOAPBody) && (parent instanceof SOAPElementImpl))
      _parent = (SOAPElementImpl) parent;
    else
      throw new SOAPException("Parent not a SOAPBody");
  }
}
