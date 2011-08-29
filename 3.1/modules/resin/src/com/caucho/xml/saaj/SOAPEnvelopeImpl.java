/*
* Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.xml.QNode;

import org.w3c.dom.Element;

public abstract class SOAPEnvelopeImpl extends SOAPElementImpl
                                       implements SOAPEnvelope
{
  protected SOAPHeader _header;
  protected SOAPBody _body;
  
  // SOAP 1.1

  protected static final NameImpl SOAP_1_1_ENVELOPE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 
                   "Envelope",
                   SOAPConstants.SOAP_ENV_PREFIX);

  protected static final NameImpl SOAP_1_1_BODY_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 
                   "Body",
                   SOAPConstants.SOAP_ENV_PREFIX);

  protected static final NameImpl SOAP_1_1_HEADER_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 
                   "Header",
                   SOAPConstants.SOAP_ENV_PREFIX);

  // SOAP 1.2
  
  protected static final NameImpl SOAP_1_2_ENVELOPE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "Envelope",
                   SOAPConstants.SOAP_ENV_PREFIX);

  protected static final NameImpl SOAP_1_2_BODY_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "Body",
                   SOAPConstants.SOAP_ENV_PREFIX);

  protected static final NameImpl SOAP_1_2_HEADER_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "Header",
                   SOAPConstants.SOAP_ENV_PREFIX);

  protected SOAPEnvelopeImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);
  }

  public SOAPBody addBody() 
    throws SOAPException
  {
    if (_body != null)
      throw new SOAPException("Envelope already contains Body");

    // addChildElement might check if _body != null for SOAP 1.2
    SOAPBody body = (SOAPBody) _factory.createElement(getBodyName());

    addChildElement(body);

    _body = body;

    return _body;
  }

  public SOAPHeader addHeader() throws SOAPException
  {
    if (_header != null)
      throw new SOAPException("Envelope already contains Header");

    _header = (SOAPHeader) _factory.createElement(getHeaderName());

    // make sure the ordering is correct if a header is inserted
    // after the body
    SOAPBody body = _body;

    if (body != null)
      body.detachNode();

    addChildElement(_header);

    if (body != null) {
      addChildElement(body);

      _body = body;
    }

    return _header;
  }

  public Name createName(String localName) 
    throws SOAPException
  {
    return _factory.createName(localName);
  }

  public Name createName(String localName, String prefix, String uri) 
    throws SOAPException
  {
    return _factory.createName(localName, prefix, uri);
  }

  public SOAPBody getBody() 
    throws SOAPException
  {
    return _body;
  }

  public SOAPHeader getHeader() 
    throws SOAPException
  {
    return _header;
  }

  public SOAPElement setElementQName(QName newName) 
    throws SOAPException
  {
    throw new SOAPException("Cannot set name of SOAP Envelope");
  }

  protected void deepCopy(Element source)
    throws SOAPException
  {
    super.deepCopy(source);

    for (SOAPNodeImpl node = _firstChild; node != null; node = node._next) {
      if (node instanceof SOAPBody) {
        _body = (SOAPBody) node;
      }
      else if (node instanceof SOAPHeader) {
        _header = (SOAPHeader) node;
      }

      if (_body != null && _header != null)
        break;
    }
  }

  protected abstract Name getBodyName();
  protected abstract Name getHeaderName();
}
