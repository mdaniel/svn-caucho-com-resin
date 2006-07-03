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
* @author Scott Ferguson
*/

package com.caucho.soap;
import javax.xml.soap.*;
import org.w3c.dom.*;
import javax.xml.namespace.*;
import java.util.*;

// XXX: abstract
public abstract class SOAPBodyImpl implements SOAPBody {

  private SOAPBody _body;

  private ArrayList<SOAPBodyElement> _elements =
    new ArrayList<SOAPBodyElement>();

  SOAPBodyImpl(SOAPBody body)
  {
    _body = body;
  }

  public SOAPBodyElement addBodyElement(Name name) throws SOAPException
  {
    /*
    SOAPBodyElement soapBodyElement = new SOAPBodyElementImpl(this, name);
    add(soapBodyElement);
    return soapBodyElement;
    */
    throw new UnsupportedOperationException();
  }

  public SOAPBodyElement addBodyElement(QName qname) throws SOAPException
  {
    /*
    SOAPBodyElement soapBodyElement = new SOAPBodyElementImpl(this, qname);
    add(soapBodyElement);
    return soapBodyElement;
    */
    throw new UnsupportedOperationException();
  }

  public SOAPBodyElement addDocument(Document document) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPFault addFault() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPFault addFault(Name faultCode, String faultString)
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPFault addFault(Name faultCode, String faultString, Locale locale)
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPFault addFault(QName faultCode, String faultString)
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPFault addFault(QName faultCode, String faultString, Locale locale)
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public Document extractContentAsDocument() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPFault getFault()
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasFault()
  {
    throw new UnsupportedOperationException();
  }

}

