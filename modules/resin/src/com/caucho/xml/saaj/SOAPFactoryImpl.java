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

import org.w3c.dom.*;
import javax.xml.soap.*;
import javax.xml.namespace.*;

public class SOAPFactoryImpl extends SOAPFactory {
  private final String _protocol;

  public SOAPFactoryImpl()
  {
    this(SOAPConstants.DEFAULT_SOAP_PROTOCOL);
  }

  public SOAPFactoryImpl(String protocol)
  {
    _protocol = protocol;
  }

  public Detail createDetail() 
    throws SOAPException
  {
    return new DetailImpl(this);
  }

  public SOAPElement createElement(Element domElement) throws SOAPException
  {
    return new SOAPElementImpl(this, domElement);
  }

  public SOAPElement createElement(Name name) throws SOAPException
  {
    if (SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(name.getURI())) {
      if ("Envelope".equals(name.getLocalName()))
        return new SOAP11EnvelopeImpl(this);
      else if ("Header".equals(name.getLocalName()))
        return new SOAPHeaderImpl(this, NameImpl.fromName(name));
      else if ("Body".equals(name.getLocalName())) 
        return new SOAPBodyImpl(this, NameImpl.fromName(name));
      else if ("Fault".equals(name.getLocalName())) 
        return new SOAP11FaultImpl(this, NameImpl.fromName(name));
    }
    else if (SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(name.getURI())) {
      if ("Envelope".equals(name.getLocalName()))
        return new SOAP12EnvelopeImpl(this);
      else if ("Header".equals(name.getLocalName()))
        return new SOAPHeaderImpl(this, NameImpl.fromName(name));
      else if ("Body".equals(name.getLocalName())) 
        return new SOAPBodyImpl(this, NameImpl.fromName(name));
      else if ("Fault".equals(name.getLocalName())) 
        return new SOAP12FaultImpl(this, NameImpl.fromName(name));
    }

    return new SOAPElementImpl(this, NameImpl.fromName(name));
  }

  public SOAPElement createElement(QName qname) throws SOAPException
  {
    return createElement((Name) NameImpl.fromQName(qname));
  }

  public SOAPElement createElement(String localName) 
    throws SOAPException
  {
    return new SOAPElementImpl(this, new NameImpl(localName));
  }

  public SOAPElement createElement(String localName, String prefix, String uri) 
    throws SOAPException
  {
    return new SOAPElementImpl(this, new NameImpl(uri, localName, prefix));
  }

  public SOAPFault createFault()
    throws SOAPException
  {
    if (SOAPConstants.SOAP_1_1_PROTOCOL.equals(_protocol))
      return new SOAP11FaultImpl(this);
    else if (SOAPConstants.SOAP_1_2_PROTOCOL.equals(_protocol))
      return new SOAP12FaultImpl(this);
    else
      throw new SOAPException("Unsupported protocol: " + _protocol);
  }

  public SOAPFault createFault(String reasonText, QName faultCode) 
    throws SOAPException
  {
    SOAPFault fault = createFault();

    fault.setFaultString(reasonText);
    fault.setFaultCode(faultCode);

    return fault;
  }

  public Name createName(String localName) 
    throws SOAPException
  {
    return new NameImpl(localName);
  }

  public Name createName(String localName, String prefix, String uri) 
    throws SOAPException
  {
    return new NameImpl(localName, prefix, uri);
  }
}
