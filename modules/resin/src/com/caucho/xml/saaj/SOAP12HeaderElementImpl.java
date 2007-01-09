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

public class SOAP12HeaderElementImpl extends SOAP11HeaderElementImpl 
{
  static final Name ENCODING_STYLE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENCODING,
                   "encodingStyle",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl NOT_UNDERSTOOD_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "NotUnderstood",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl SOAP_1_2_UPGRADE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "Upgrade",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl SOAP_1_2_MUST_UNDERSTAND_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "mustUnderstand",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl RELAY_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "relay",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl ROLE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 
                   "role",
                   SOAPConstants.SOAP_ENV_PREFIX);

  SOAP12HeaderElementImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);

    _mustUnderstandName = SOAP_1_2_MUST_UNDERSTAND_NAME;
  }

  SOAP12HeaderElementImpl(SOAPFactory factory, SOAPElement element)
    throws SOAPException
  {
    this(factory, NameImpl.fromQName(element.getElementQName()));

    copySOAPElement(element);
  }

  // override encoding style so that the namespace is SOAP 1.2
  public String getEncodingStyle()
  {
    return getAttributeValue(ENCODING_STYLE_NAME);
  }

  public void setEncodingStyle(String encodingStyle) 
    throws SOAPException
  {
    if (! SOAPConstants.URI_NS_SOAP_ENCODING.equals(encodingStyle) &&
        ! SOAPConstants.URI_NS_SOAP_1_2_ENCODING.equals(encodingStyle))
      throw new IllegalArgumentException("Unknown SOAP Encoding: " + encodingStyle);

    addAttribute(ENCODING_STYLE_NAME, encodingStyle);
  }

  public String getActor()
  {
    return getRole();
  }

  public void setActor(String actorURI)
  {
    try {
      setRole(actorURI);
    }
    catch (SOAPException e) {
      // as specified by SAAJ
      throw new IllegalArgumentException(e);
    }
  }

  public boolean getRelay()
  {
    return "true".equalsIgnoreCase(getAttributeValue((Name) RELAY_NAME));
  }

  public void setRelay(boolean relay) 
    throws SOAPException
  {
    addAttribute((Name) RELAY_NAME, Boolean.toString(relay));
  }

  public String getRole()
  {
    return getAttributeValue((Name) ROLE_NAME);
  }

  public void setRole(String uri) 
    throws SOAPException
  {
    addAttribute((Name) ROLE_NAME, uri);
  }

  public void setParentElement(SOAPElement parent)
    throws SOAPException
  {
    if (parent == null)
      throw new IllegalArgumentException();

    if (parent instanceof SOAP12HeaderImpl)
      _parent = (SOAP12HeaderImpl) parent;
    else if (parent instanceof SOAP12HeaderElementImpl)
      _parent = (SOAP12HeaderElementImpl) parent;
    else
      throw new SOAPException("Parent is a " + parent.getClass() + ", not a SOAPHeader or SOAPHeaderElement");
  }
}
