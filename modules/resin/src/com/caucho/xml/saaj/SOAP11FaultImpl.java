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

import javax.xml.namespace.*;
import javax.xml.soap.*;
import java.util.*;

import com.caucho.util.L10N;

/**
 * 
 **/
public class SOAP11FaultImpl extends SOAPBodyElementImpl 
                             implements SOAPFault 
{
  private static final L10N L = new L10N(SOAP11FaultImpl.class);

  private static final NameImpl SOAP_1_1_FAULT_NAME = 
    new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 
                 "Fault",
                 SOAPConstants.SOAP_ENV_PREFIX);

  private static final Name FAULT_CODE = new NameImpl("faultcode");
  private static final Name FAULT_STRING = new NameImpl("faultstring");
  private static final Name FAULT_ACTOR = new NameImpl("faultactor");

  protected Detail _detail;
  protected SOAPElement _faultActor;
  protected SOAPElement _faultCode;
  protected SOAPElement _faultString;
  protected Locale _faultLocale;

  SOAP11FaultImpl(SOAPFactory factory)
    throws SOAPException
  {
    this(factory, SOAP_1_1_FAULT_NAME);
  }

  SOAP11FaultImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);
    /*

    _faultCode = _factory.createElement(FAULT_CODE);
    _faultString = _factory.createElement(FAULT_STRING);

    addChildElement(_faultCode);
    addChildElement(_faultString);*/
  }

  public SOAPElement addChildElement(SOAPElement element) 
    throws SOAPException
  {
    if (FAULT_CODE.getLocalName().equals(element.getLocalName()) &&
        element.getNamespaceURI() == null ||
        "".equals(element.getNamespaceURI())) {
      _faultCode = element;
    }
    else if (FAULT_STRING.getLocalName().equals(element.getLocalName()) &&
             element.getNamespaceURI() == null ||
             "".equals(element.getNamespaceURI())) {
      _faultString = element;
    }

    return super.addChildElement(element);
  }

  // Detail

  public Detail addDetail() 
    throws SOAPException
  {
    if (_detail != null)
      throw new SOAPException("Fault already contains a valid Detail");

    _detail = new SOAP11DetailImpl(_factory);

    return _detail;
  }

  public Detail getDetail()
  {
    return _detail;
  }

  public boolean hasDetail()
  {
    return _detail != null;
  }

  // reason

  public void addFaultReasonText(String text, Locale locale) 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Fault reasons unsupported in SOAP 1.1");
  }

  public Iterator getFaultReasonLocales() 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Fault reasons unsupported in SOAP 1.1");
  }

  public String getFaultReasonText(Locale locale) 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Fault reasons unsupported in SOAP 1.1");
  }

  public Iterator getFaultReasonTexts() 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Fault reasons unsupported in SOAP 1.1");
  }

  // subcode

  public void appendFaultSubcode(QName subcode) 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Subcodes unsupported in SOAP 1.1");
  }

  public Iterator getFaultSubcodes()
  {
    throw new UnsupportedOperationException("Subcodes unsupported in SOAP 1.1");
  }

  public void removeAllFaultSubcodes()
  {
    throw new UnsupportedOperationException("Subcodes unsupported in SOAP 1.1");
  }

  // actor

  public void setFaultActor(String faultActor) 
    throws SOAPException
  {
    if (_faultActor == null) {
      _faultActor = _factory.createElement(FAULT_ACTOR);
      addChildElement(_faultActor);
    }

    _faultActor.setValue(faultActor);
  }

  public String getFaultActor()
  {
    if (_faultActor == null)
      return null;

    return _faultActor.getValue();
  }

  // faultcode

  public String getFaultCode()
  {
    if (_faultCode == null)
      return null;

    return _faultCode.getValue();
  }

  public Name getFaultCodeAsName()
  {
    String faultcode = getFaultCode();

    if (_faultCode == null)
      return null;

    int colon = faultcode.indexOf(':');

    if (colon >= 0) {
      String prefix = faultcode.substring(0, colon);
      String localName = faultcode.substring(colon + 1);
      String uri = _faultCode.getNamespaceURI(prefix);

      return new NameImpl(uri, localName, prefix);
    }

    return new NameImpl(faultcode);
  }

  public QName getFaultCodeAsQName()
  {
    return (NameImpl) getFaultCodeAsName();
  }

  public void setFaultCode(Name faultCodeName) 
    throws SOAPException
  {
    if (faultCodeName.getPrefix() == null || 
        "".equals(faultCodeName.getPrefix()))
      throw new SOAPException(L.l("Fault codes must have qualified names.  Name given: {0}", faultCodeName));

    if (_faultCode == null) {
      _faultCode = _factory.createElement(FAULT_CODE);
      addChildElement(_faultCode);
    }

    if (getNamespaceURI(faultCodeName.getPrefix()) == null) {
      _faultCode.addNamespaceDeclaration(faultCodeName.getPrefix(), 
                                         faultCodeName.getURI());
    }

    _faultCode.setValue(faultCodeName.getQualifiedName());
  }

  public void setFaultCode(QName faultCodeQName) 
    throws SOAPException
  {
    setFaultCode((Name) NameImpl.fromQName(faultCodeQName));
  }

  public void setFaultCode(String faultCode) 
    throws SOAPException
  {
    /* XXX Should we check for a QName here?
    if (faultCode.indexOf(':') < 0)
      throw new SOAPException(L.l("Fault codes must have qualified names.  Name given: {0}", faultCode));
*/
    if (_faultCode == null) {
      _faultCode = _factory.createElement(FAULT_CODE);
      addChildElement(_faultCode);
    }

    _faultCode.setValue(faultCode);
  }

  // faultstring

  public String getFaultString()
  {
    if (_faultString == null)
      return null;

    return _faultString.getValue();
  }

  public void setFaultString(String faultString)
    throws SOAPException
  {
    if (_faultString == null) {
      _faultString = _factory.createElement(FAULT_STRING);
      addChildElement(_faultString);
    }

    _faultString.setValue(faultString);
  }

  public void setFaultString(String faultString, Locale locale) 
    throws SOAPException
  {
    if (_faultString == null) {
      _faultString = _factory.createElement(FAULT_STRING);
      addChildElement(_faultString);
    }

    _faultString.setValue(faultString);
    _faultLocale = locale;
  }

  public Locale getFaultStringLocale()
  {
    return _faultLocale;
  }

  // faultnode

  public String getFaultNode()
  {
    throw new UnsupportedOperationException("Fault nodes unsupported in SOAP 1.1");
  }

  public void setFaultNode(String uri) 
    throws SOAPException
  {
    throw new UnsupportedOperationException("Fault nodes unsupported in SOAP 1.1");
  }

  // faultrole 

  public String getFaultRole()
  {
    throw new UnsupportedOperationException("Fault role unsupported in SOAP 1.1");
  }
  
  public void setFaultRole(String uri)
    throws SOAPException
  {
    throw new UnsupportedOperationException("Fault role unsupported in SOAP 1.1");
  }
}
