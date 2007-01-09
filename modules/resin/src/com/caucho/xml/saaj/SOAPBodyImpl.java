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

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.*;
import javax.xml.soap.*;

import org.w3c.dom.*;

import com.caucho.xml.QDocument;
import com.caucho.xml.QNode;

public class SOAPBodyImpl extends SOAPElementImpl
                          implements SOAPBody 
{
  private SOAPFault _fault;

  SOAPBodyImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);
  }

  public SOAPElement addChildElement(Name name) 
    throws SOAPException
  {
    return addBodyElement(name);
  }

  public SOAPElement addChildElement(SOAPElement element) 
    throws SOAPException
  {
    SOAPElement child = element;

    if (! (element instanceof SOAPBodyElement))
      child = new SOAPBodyElementImpl(_factory, element);

    return super.addChildElement(child);
  }

  public SOAPBodyElement addBodyElement(Name name) 
    throws SOAPException
  {
    SOAPBodyElement child = 
      new SOAPBodyElementImpl(_factory, NameImpl.fromName(name));

    appendChild(child);

    return child;
  }

  public SOAPBodyElement addBodyElement(QName qname) 
    throws SOAPException
  {
    return addBodyElement((Name) NameImpl.fromQName(qname));
  }

  public SOAPBodyElement addDocument(Document document) 
    throws SOAPException
  {
    SOAPBodyElement child = 
      new SOAPBodyElementImpl(_factory, document.getDocumentElement());

    super.addChildElement(child);

    return child;
  }

  public SOAPFault addFault() 
    throws SOAPException
  {
    // XXX replace or throw exception?
    _fault = _factory.createFault();

    return _fault;
  }

  public SOAPFault addFault(Name faultCode, String faultString) 
    throws SOAPException
  {
    _fault = _factory.createFault(faultString, NameImpl.toQName(faultCode));

    return _fault;
  }

  public SOAPFault addFault(Name faultCode, String faultString, Locale locale) 
    throws SOAPException
  {
    _fault = _factory.createFault();

    _fault.setFaultString(faultString, locale);
    _fault.setFaultCode(faultCode);

    return _fault;
  }

  public SOAPFault addFault(QName faultCode, String faultString) 
    throws SOAPException
  {
    _fault = _factory.createFault(faultString, faultCode);

    return _fault;
  }

  public SOAPFault addFault(QName faultCode, String faultString, Locale locale) 
    throws SOAPException
  {
    _fault = _factory.createFault();

    _fault.setFaultString(faultString, locale);
    _fault.setFaultCode(faultCode);

    return _fault;
  }

  public Document extractContentAsDocument() 
    throws SOAPException
  {
    if (getFirstChild() == null || getFirstChild() != getLastChild())
      throw new SOAPException("Body does not have exactly one child");

    Element child = (Element) getFirstChild();
    removeContents();

    // XXX
    QDocument document = new QDocument();
    document.setDocumentElement(child);

    return document;
  }

  public SOAPFault getFault()
  {
    return _fault;
  }

  public boolean hasFault()
  {
    return _fault != null;
  }

  public SOAPElement setElementQName(QName newName) 
    throws SOAPException
  {
    throw new SOAPException("Cannot set name of SOAP Body");
  }

  public void detachNode()
  {
    if (getParentNode() instanceof SOAPEnvelopeImpl) {
      SOAPEnvelopeImpl parent = (SOAPEnvelopeImpl) getParentNode();
      parent._body = null;
    }

    super.detachNode();
  }
}
