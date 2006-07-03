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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.soap;

import com.caucho.log.*;
//import com.caucho.xml.*;
//import org.xml.sax.*;
import javax.xml.namespace.*;
import javax.xml.bind.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * An HTTP Servlet to dispatch SOAP requests
 */
public class SOAPServlet extends HttpServlet {

  public final static String XMLNS =
    "http://www.w3.org/2000/xmlns/";
  public final static String SOAP_ENVELOPE =
    "http://www.w3.org/2003/05/soap-envelope";
  public final static String SOAP_ENCODING =
    "http://schemas.xmlsoap.org/soap/encoding/";

  private SOAPHandler<SOAPMessageContext> _soapHandler;
  
  public SOAPServlet(SOAPHandler<SOAPMessageContext> soapHandler)
  {
    _soapHandler = soapHandler;
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    // XXX: show that nifty debugging screen like Sun does
  }
	   
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    // XXX: check content-type
    SOAPMessage soapMessage =
      null;
      //new SOAPMessageImpl();

    SOAPMessageContext soapMessageContext =
      new SOAPMessageContextImpl(soapMessage);

    // XXX: check return code
    _soapHandler.handleMessage(soapMessageContext);

    // XXX: set content-type
    OutputStream os = resp.getOutputStream();
    try {
      soapMessage.writeTo(os);
    }
    catch (SOAPException e) {
      throw new ServletException(e);
    }
    finally {
      os.close();
    }
  }

  private static class SOAPMessageContextImpl
    extends HashMap<String,Object>
    implements SOAPMessageContext {

    private SOAPMessage _soapMessage;

    public SOAPMessageContextImpl(SOAPMessage soapMessage)
    {
      this._soapMessage = soapMessage;
    }

    public MessageContext.Scope getScope(String name)
    {
      throw new UnsupportedOperationException();
    }
      
    public void setScope(String name, MessageContext.Scope scope)
    {
      throw new UnsupportedOperationException();
    }
      
    public Object[] getHeaders(QName header,
			       JAXBContext context, boolean allRoles)
    {
      throw new UnsupportedOperationException();
    }

    public SOAPMessage getMessage()
    {
      return _soapMessage;
    }

    public List<String> getRoles()
    {
      throw new UnsupportedOperationException();
    }

    public void setMessage(SOAPMessage message)
    {
      _soapMessage = message;
    }
      
  }

}


