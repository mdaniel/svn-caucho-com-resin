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

public class SOAPHeaderImpl extends SOAPElementImpl
                            implements SOAPHeader 
{
  private static final NameImpl SUPPORTED_ENVELOPE_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "SupportedEnvelope");
  private static final NameImpl QNAME_NAME = new NameImpl("qname");

  SOAPHeaderImpl(SOAPFactory factory, NameImpl name)
  {
    super(factory, name);
  }

  public SOAPElement addChildElement(Name name) 
    throws SOAPException
  {
    return addHeaderElement(name);
  }

  public SOAPElement addChildElement(SOAPElement element) 
    throws SOAPException
  {
    SOAPElement child = element;

    if (! (element instanceof SOAPHeaderElement))
      element = new SOAPHeaderElementImpl(_factory, element);

    return super.addChildElement(element);
  }

  public SOAPHeaderElement addHeaderElement(Name name) 
    throws SOAPException
  {
    SOAPHeaderElement child = 
      new SOAPHeaderElementImpl(_factory, NameImpl.fromName(name));

    appendChild(child);

    return child;
  }

  public SOAPHeaderElement addHeaderElement(QName qname) 
    throws SOAPException
  {
    return addHeaderElement((Name) NameImpl.fromQName(qname));
  }

  public SOAPHeaderElement addNotUnderstoodHeaderElement(QName name) 
    throws SOAPException
  {
    // NotUnderstood elements look like:
    // <soapenv:NotUnderstood qname="abc:Extension" xmlns:abc="http://ns/"/>
    //
    SOAPHeaderElement element = 
      new SOAPHeaderElementImpl(_factory,
                                SOAPHeaderElementImpl.NOT_UNDERSTOOD_NAME);

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

  public SOAPHeaderElement addUpgradeHeaderElement(Iterator supportedSoapUris)
    throws SOAPException
  {
    // E.G.
    // <env:Upgrade>
    //   <env:SupportedEnvelope qname="ns1:Envelope" 
    //              xmlns:ns1="http://www.w3.org/2003/05/soap-envelope"/>
    //   <env:SupportedEnvelope qname="ns2:Envelope" 
    //          xmlns:ns2="http://schemas.xmlsoap.org/soap/envelope/"/>
    // </env:Upgrade>

    SOAPHeaderElement element = 
      new SOAPHeaderElementImpl(_factory, SOAPHeaderElementImpl.UPGRADE_NAME);

    int i = 1;

    while (supportedSoapUris.hasNext()) {
      String uri = supportedSoapUris.next().toString();
      element.addChildElement(createSupportedElement(uri, i++));
    }

    addChildElement(element);

    return element;
  }

  public SOAPHeaderElement addUpgradeHeaderElement(String supportedSoapUri) 
    throws SOAPException
  {
    SOAPHeaderElement element = 
      new SOAPHeaderElementImpl(_factory, SOAPHeaderElementImpl.UPGRADE_NAME);

    element.addChildElement(createSupportedElement(supportedSoapUri, 1));

    addChildElement(element);

    return element;
  }

  public SOAPHeaderElement addUpgradeHeaderElement(String[] supportedSoapUris)
    throws SOAPException
  {
    SOAPHeaderElement element = 
      new SOAPHeaderElementImpl(_factory, SOAPHeaderElementImpl.UPGRADE_NAME);

    for (int i = 0; i < supportedSoapUris.length; i++) {
      String uri = supportedSoapUris[i];
      element.addChildElement(createSupportedElement(uri, i++));
    }

    addChildElement(element);

    return element;
  }

  private SOAPElement createSupportedElement(String uri, int i)
    throws SOAPException
  {
    SOAPElement supported = 
      new SOAPHeaderElementImpl(_factory, SUPPORTED_ENVELOPE_NAME);

    supported.addAttribute((Name) QNAME_NAME, "ns" + i + ":Envelope");
    supported.addNamespaceDeclaration("ns" + i, uri);

    return supported;
  }

  public Iterator examineAllHeaderElements()
  {
    return getChildElements();
  }

  public Iterator examineHeaderElements(String actor)
  {
    throw new UnsupportedOperationException();
  }

  public Iterator examineMustUnderstandHeaderElements(String actor)
  {
    throw new UnsupportedOperationException();
  }

  public Iterator extractAllHeaderElements()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator extractHeaderElements(String actor)
  {
    throw new UnsupportedOperationException();
  }
}
