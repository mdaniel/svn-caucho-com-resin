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

import com.caucho.xml.QNode;

public class SOAP12EnvelopeImpl extends SOAPEnvelopeImpl
{
  static final NameImpl ENCODING_STYLE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE,
                   "encodingStyle",
                   SOAPConstants.SOAP_ENV_PREFIX);

  SOAP12EnvelopeImpl(SOAPFactory factory)
    throws SOAPException
  {
    super(factory, SOAP_1_2_ENVELOPE_NAME);
  }

  protected Name getBodyName()
  {
    return SOAP_1_2_BODY_NAME;
  }

  protected Name getHeaderName()
  {
    return SOAP_1_2_HEADER_NAME;
  }

  public String getEncodingStyle()
    throws SOAPException
  {
    // optimization
    return null;
  }

  public void setEncodingStyle(String encodingStyle) 
    throws SOAPException
  {
    throw new SOAPException("encodingStyle illegal for this element");
  }

  public SOAPElement addAttribute(Name name, String value) 
    throws SOAPException
  {
    if (name.equals(ENCODING_STYLE_NAME))
      throw new SOAPException("encodingStyle illegal for this element");

    return super.addAttribute(name, value);
  }

  public SOAPElement addAttribute(QName qname, String value) 
    throws SOAPException
  {
    if (qname.equals(ENCODING_STYLE_NAME))
      throw new SOAPException("encodingStyle illegal for this element");

    return super.addAttribute(qname, value);
  }

  public SOAPElement addChildElement(Name name)
    throws SOAPException
  {
    if (_body != null)
      throw new SOAPException("SOAP 1.2 does not allow elements after the body");

    return super.addChildElement(name);
  }

  public SOAPElement addChildElement(QName qname)
    throws SOAPException
  {
    if (_body != null)
      throw new SOAPException("SOAP 1.2 does not allow elements after the body");

    return super.addChildElement(qname);
  }

  public SOAPElement addChildElement(SOAPElement element)
    throws SOAPException
  {
    if (_body != null)
      throw new SOAPException("SOAP 1.2 does not allow elements after the body");

    return super.addChildElement(element);
  }

  public SOAPElement addChildElement(String localName)
    throws SOAPException
  {
    if (_body != null)
      throw new SOAPException("SOAP 1.2 does not allow elements after the body");

    return super.addChildElement(localName);
  }

  public SOAPElement addChildElement(String localName, String prefix)
    throws SOAPException
  {
    if (_body != null)
      throw new SOAPException("SOAP 1.2 does not allow elements after the body");

    return super.addChildElement(localName, prefix);
  }

  public SOAPElement addChildElement(String localName, 
                                     String prefix, 
                                     String uri)
    throws SOAPException
  {
    if (_body != null)
      throw new SOAPException("SOAP 1.2 does not allow elements after the body");

    return super.addChildElement(localName, prefix, uri);
  }
}
