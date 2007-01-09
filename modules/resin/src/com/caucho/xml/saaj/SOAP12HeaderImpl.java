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

public class SOAP12HeaderImpl extends SOAP11HeaderImpl
{
  private static final Name ENCODING_STYLE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENCODING,
                   "encodingStyle",
                   SOAPConstants.SOAP_ENV_PREFIX);

  private static final NameImpl QNAME_NAME = new NameImpl("qname");

  SOAP12HeaderImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);

    _supportedEnvelopeName = SOAP_1_2_SUPPORTED_ENVELOPE_NAME;
    _upgradeName = SOAP12HeaderElementImpl.SOAP_1_2_UPGRADE_NAME;
  }

  public SOAPElement addTextNode(String text) 
    throws SOAPException
  {
    if (text.indexOf('<') >= 0)
      throw new SOAPException("Child tags not allowed in the Text nodes of SOAP 1.2 headers");

    appendChild(new TextImpl(_factory, text));

    return this;
  }

  public String getEncodingStyle()
    throws SOAPException
  {
    throw new SOAPException("encodingStyle illegal for this element");
  }

  public void setEncodingStyle(String encodingStyle) 
    throws SOAPException
  {
    throw new SOAPException("encodingStyle illegal for this element");
  }


  public SOAPElement addChildElement(SOAPElement element) 
    throws SOAPException
  {
    SOAPElement child = element;

    if (! (element instanceof SOAPHeaderElement))
      element = new SOAP12HeaderElementImpl(_factory, element);

    return super.addChildElement(element);
  }

  public SOAPHeaderElement addHeaderElement(Name name) 
    throws SOAPException
  {
    SOAPHeaderElement child = 
      new SOAP12HeaderElementImpl(_factory, NameImpl.fromName(name));

    appendChild(child);

    return child;
  }

  public SOAPHeaderElement addNotUnderstoodHeaderElement(QName name) 
    throws SOAPException
  {
    // NotUnderstood elements look like:
    // <soapenv:NotUnderstood qname="abc:Extension" xmlns:abc="http://ns/"/>
    //
    SOAPHeaderElement element = 
      createHeaderElement(SOAP12HeaderElementImpl.NOT_UNDERSTOOD_NAME);

    if (name.getPrefix() == null)
      throw new SOAPException("No prefix given with NotUnderstood header");
    else if (name.getNamespaceURI() == null)
      throw new SOAPException("No namespace given with NotUnderstood header");

    element.addAttribute((Name) QNAME_NAME, 
                         name.getPrefix() + ':' + name.getLocalPart());
    element.addNamespaceDeclaration(name.getPrefix(), name.getNamespaceURI());

    addChildElement(element);

    return element;
  }

  protected SOAPHeaderElement createHeaderElement(NameImpl name)
    throws SOAPException
  {
    return new SOAP12HeaderElementImpl(_factory, name);
  }
}
