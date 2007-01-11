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

public class SOAP11HeaderImpl extends SOAPElementImpl
                            implements SOAPHeader 
{
  private static final NameImpl QNAME_NAME = new NameImpl("qname");

  protected static final NameImpl SOAP_1_1_SUPPORTED_ENVELOPE_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "SupportedEnvelope");

  protected static final NameImpl SOAP_1_2_SUPPORTED_ENVELOPE_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "SupportedEnvelope");

  protected NameImpl _supportedEnvelopeName;
  protected NameImpl _upgradeName;

  SOAP11HeaderImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);

    _supportedEnvelopeName = SOAP_1_1_SUPPORTED_ENVELOPE_NAME;
    _upgradeName = SOAP11HeaderElementImpl.SOAP_1_1_UPGRADE_NAME;
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
      element = new SOAP11HeaderElementImpl(_factory, element);

    return super.addChildElement(element);
  }

  public SOAPHeaderElement addHeaderElement(Name name) 
    throws SOAPException
  {
    SOAPHeaderElement child = 
      new SOAP11HeaderElementImpl(_factory, NameImpl.fromName(name));

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
    throw new UnsupportedOperationException("NotUnderstood Header element not supported by SOAP 1.1");
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

    SOAPHeaderElement element = createHeaderElement(_upgradeName);

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
    SOAPHeaderElement element = createHeaderElement(_upgradeName);

    element.addChildElement(createSupportedElement(supportedSoapUri, 1));

    addChildElement(element);

    return element;
  }

  public SOAPHeaderElement addUpgradeHeaderElement(String[] supportedSoapUris)
    throws SOAPException
  {
    SOAPHeaderElement element = createHeaderElement(_upgradeName);

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
    SOAPElement supported = createHeaderElement(_supportedEnvelopeName);

    supported.addAttribute((Name) QNAME_NAME, "ns" + i + ":Envelope");
    supported.addNamespaceDeclaration("ns" + i, uri);

    return supported;
  }

  protected SOAPHeaderElement createHeaderElement(NameImpl name)
    throws SOAPException
  {
    return new SOAP11HeaderElementImpl(_factory, name);
  }

  public Iterator examineAllHeaderElements()
  {
    return getChildElements();
  }

  public Iterator examineHeaderElements(String actor)
  {
    return new HeaderElementIterator(actor);
  }

  public Iterator examineMustUnderstandHeaderElements(String actor)
  {
    return new HeaderElementIterator(actor, true);
  }

  public Iterator extractAllHeaderElements()
  {
    ArrayList<SOAPHeaderElement> elements = new ArrayList<SOAPHeaderElement>();
    Iterator i = examineAllHeaderElements();

    while (i.hasNext()) {
      SOAPHeaderElement element = (SOAPHeaderElement) i.next();

      element.detachNode();
      elements.add(element);
    }

    return elements.iterator();
  }

  public Iterator extractHeaderElements(String actor)
  {
    ArrayList<SOAPHeaderElement> elements = new ArrayList<SOAPHeaderElement>();
    Iterator i = examineAllHeaderElements();

    while (i.hasNext()) {
      SOAPHeaderElement element = (SOAPHeaderElement) i.next();

      if (actor.equals(element.getActor())) {
        element.detachNode();
        elements.add(element);
      }
    }

    return elements.iterator();
  }

  public SOAPElement setElementQName(QName newName) 
    throws SOAPException
  {
    throw new SOAPException("Cannot set name of SOAP Header");
  }

  public void detachNode()
  {
    if (getParentNode() instanceof SOAPEnvelopeImpl) {
      SOAPEnvelopeImpl parent = (SOAPEnvelopeImpl) getParentNode();
      parent._header = null;
    }

    super.detachNode();
  }

  protected class HeaderElementIterator implements Iterator
  { 
    private String _actor;
    private boolean _mustUnderstand;
    private SOAPHeaderElement _headerElement;

    public HeaderElementIterator(String actor)
    {
      this(actor, false);
    }

    public HeaderElementIterator(String actor, boolean mustUnderstand)
    {
      _actor = actor;
      _mustUnderstand = mustUnderstand;

      advanceHeaderElement((SOAPHeaderElement) _firstChild);
    }

    public boolean hasNext()
    {
      return _headerElement != null;
    }

    public Object next()
    {
      if (_headerElement == null)
        throw new NoSuchElementException();

      SOAPHeaderElement next = _headerElement;

      advanceHeaderElement((SOAPHeaderElement) _headerElement.getNextSibling());

      return next;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    private void advanceHeaderElement(SOAPHeaderElement next)
    {
      for (_headerElement = next;
           _headerElement != null; 
           _headerElement = (SOAPHeaderElement) _headerElement.getNextSibling())
      {
        if (_actor.equals(_headerElement.getActor())) {
          if (_mustUnderstand) {
            if (_headerElement.getMustUnderstand())
              break;
          }
          else
            break;
        }
      }
    }
  }
}
