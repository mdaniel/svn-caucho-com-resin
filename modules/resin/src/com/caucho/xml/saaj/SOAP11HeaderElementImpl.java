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

public class SOAP11HeaderElementImpl extends SOAPElementImpl
                                     implements SOAPHeaderElement 
{
  static final NameImpl SOAP_1_1_UPGRADE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 
                   "Upgrade",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl SOAP_1_1_MUST_UNDERSTAND_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 
                   "mustUnderstand",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl ACTOR_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 
                   "actor",
                   SOAPConstants.SOAP_ENV_PREFIX);

  static final NameImpl NOT_UNDERSTOOD_NAME 
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "NotUnderstood");

  static final NameImpl UPGRADE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Upgrade");

  protected Name _mustUnderstandName = SOAP_1_1_MUST_UNDERSTAND_NAME;

  SOAP11HeaderElementImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);
  }

  SOAP11HeaderElementImpl(SOAPFactory factory, SOAPElement element)
    throws SOAPException
  {
    this(factory, NameImpl.fromQName(element.getElementQName()));

    copySOAPElement(element);
  }

  public String getActor()
  {
    return getAttributeValue((Name) ACTOR_NAME);
  }

  public void setActor(String actorURI)
  {
    try {
      addAttribute((Name) ACTOR_NAME, actorURI);
    }
    catch (SOAPException e) {
      // as specified by SAAJ
      throw new IllegalArgumentException(e);
    }
  }

  public boolean getMustUnderstand()
  {
    String value = getAttributeValue(_mustUnderstandName);

    return "true".equalsIgnoreCase(value);
  }

  public void setMustUnderstand(boolean mustUnderstand)
  {
    try {
      addAttribute(_mustUnderstandName, Boolean.toString(mustUnderstand));
    }
    catch (SOAPException e) {
      // as specified by SAAJ
      throw new IllegalArgumentException(e);
    }
  }

  public boolean getRelay()
  {
    throw new UnsupportedOperationException("Relay unsupported by SOAP 1.1");
  }

  public void setRelay(boolean relay) 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Relay unsupported by SOAP 1.1");
  }

  public String getRole()
  {
    throw new UnsupportedOperationException("Relay unsupported by SOAP 1.1");
  }

  public void setRole(String uri) 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Relay unsupported by SOAP 1.1");
  }

  public void setParentElement(SOAPElement parent)
    throws SOAPException
  {
    if (parent == null)
      throw new IllegalArgumentException();

    if (parent instanceof SOAP11HeaderImpl)
      _parent = (SOAP11HeaderImpl) parent;
    else if (parent instanceof SOAP11HeaderElementImpl)
      _parent = (SOAP11HeaderElementImpl) parent;
    else
      throw new SOAPException("Parent is a " + parent.getClass() + ", not a SOAPHeader or SOAPHeaderElement");
  }
}
