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

import javax.xml.namespace.*;
import javax.xml.soap.*;
import java.util.*;

public class SOAP12FaultImpl extends SOAP11FaultImpl {
  private static final NameImpl SOAP_1_2_FAULT_NAME = 
    new NameImpl(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");

  private String _faultNode;
  private String _faultRole;
  private final ArrayList<SOAPElement> _subcodes = new ArrayList<SOAPElement>();
  private final Map<Locale,String> _faultReasons = new HashMap<Locale,String>();

  SOAP12FaultImpl(SOAPFactory factory)
    throws SOAPException
  {
    this(factory, SOAP_1_2_FAULT_NAME);
  }

  SOAP12FaultImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);
  }

  // subcode

  public void appendFaultSubcode(QName subcode) 
    throws SOAPException
  {
    SOAPElement element = _factory.createElement(subcode);

    _subcodes.add(element);
    _faultCode.addChildElement(element);
  }

  public Iterator getFaultSubcodes()
  {
    return new SubcodeIterator();
  }

  public void removeAllFaultSubcodes()
  {
    for (int i = 0; i < _subcodes.size(); i++)
      _faultCode.removeChild(_subcodes.get(i));

    _subcodes.clear();
  }

  // reason

  public void addFaultReasonText(String text, Locale locale) 
    throws SOAPException
  {
    _faultReasons.put(locale, text);
  }

  public Iterator getFaultReasonLocales() 
    throws SOAPException
  {
    return _faultReasons.keySet().iterator();
  }

  public String getFaultReasonText(Locale locale) 
    throws SOAPException
  {
    return _faultReasons.get(locale);
  }

  public Iterator getFaultReasonTexts() 
    throws SOAPException
  {
    return _faultReasons.values().iterator();
  }

  // faultstring

  public String getFaultString()
  {
    String reason = null;

    try {
      reason = (String) getFaultReasonTexts().next();
    } 
    catch (SOAPException e) {}

    return reason;
  }

  public Locale getFaultStringLocale()
  {
    Locale locale = null;

    try {
      locale = (Locale) getFaultReasonLocales().next();
    }
    catch (SOAPException e) {}

    return locale;
  }

  public void setFaultString(String faultString)
    throws SOAPException
  {
    addFaultReasonText(faultString, Locale.getDefault());
  }

  public void setFaultString(String faultString, Locale locale) 
    throws SOAPException
  {
    addFaultReasonText(faultString, locale);
  }

  // faultnode

  public String getFaultNode()
  {
    return _faultNode;
  }

  public void setFaultNode(String uri) 
    throws SOAPException
  {
    _faultNode = uri;
  }

  // faultrole 

  public String getFaultRole()
  {
    return _faultRole;
  }
  
  public void setFaultRole(String uri)
    throws SOAPException
  {
    _faultRole = uri;
  }

  private class SubcodeIterator implements Iterator<QName> {
    private Iterator<SOAPElement> _iterator;

    public SubcodeIterator()
    {
      _iterator = _subcodes.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public QName next()
    {
      return _iterator.next().getElementQName();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
