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

import javax.xml.XMLConstants;
import javax.xml.namespace.*;
import javax.xml.soap.*;

import com.caucho.xml.QNode;

public class SOAPHeaderElementImpl extends SOAPElementImpl
                                   implements SOAPHeaderElement 
{
  static final NameImpl NOT_UNDERSTOOD_NAME =
    new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "NotUnderstood");

  static final NameImpl UPGRADE_NAME =
    new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Upgrade");

  // XXX 1.1 vs 1.2 
  private String _actor;
  private boolean _mustUnderstand;
  private boolean _relay;
  private String _role;

  SOAPHeaderElementImpl(SOAPFactory factory, NameImpl name)
  {
    super(factory, name);
  }

  SOAPHeaderElementImpl(SOAPFactory factory, SOAPElement element)
  {
    this(factory, NameImpl.fromQName(element.getElementQName()));

    copySOAPElement(element);
  }

  public String getActor()
  {
    return _actor;
  }

  public void setActor(String actorURI)
  {
    _actor = actorURI;
  }

  public boolean getMustUnderstand()
  {
    return _mustUnderstand;
  }

  public void setMustUnderstand(boolean mustUnderstand)
  {
    _mustUnderstand = mustUnderstand;
  }

  public boolean getRelay()
  {
    return _relay;
  }

  public void setRelay(boolean relay) 
    throws SOAPException
  {
    _relay = relay;
  }

  public String getRole()
  {
    return _role;
  }

  public void setRole(String uri) 
    throws SOAPException
  {
    _role = uri;
  }
}
