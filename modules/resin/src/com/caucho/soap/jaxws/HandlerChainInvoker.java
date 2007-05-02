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

package com.caucho.soap.jaxws;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;

import javax.xml.stream.XMLStreamException;

import javax.xml.soap.SOAPException;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.ws.*;
import javax.xml.ws.handler.*;
import static javax.xml.ws.handler.MessageContext.*;
import javax.xml.ws.handler.soap.*;

import com.caucho.util.L10N;

import com.caucho.xml.stream.StaxUtil;
import com.caucho.xml.stream.XMLStreamReaderImpl;
import com.caucho.xml.stream.XMLStreamWriterImpl;

/**
 * Responsible for invoking a handler chain.
 **/
public class HandlerChainInvoker {
  private final static Logger log = 
    Logger.getLogger(HandlerChainInvoker.class.getName());
  private final static L10N L = new L10N(HandlerChainInvoker.class);

  private final List<Handler> _chain;
  private final BindingProvider _bindingProvider;

  // maintains state over a request-response pattern
  private final boolean[] _invoked;

  private static Transformer _transformer;

  public HandlerChainInvoker(List<Handler> chain, 
                             BindingProvider bindingProvider)
  {
    _chain = chain;
    _invoked = new boolean[_chain.size()];
    _bindingProvider = bindingProvider;
  }

  /**
   * Invoke the handler chain for an outbound message.
   **/
  public void invoke(Source source, OutputStream out, 
                     Map<String,DataHandler> attachments,
                     Map<String,Object> httpProperties,
                     boolean close, boolean request)
    throws WebServiceException
  {
    LogicalMessageContextImpl logicalContext = new LogicalMessageContextImpl();
    SOAPMessageContextImpl soapContext = new SOAPMessageContextImpl();

    // Set the mandatory properties
    logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);
    soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);

    logicalContext.put(OUTBOUND_MESSAGE_ATTACHMENTS, attachments);
    soapContext.put(OUTBOUND_MESSAGE_ATTACHMENTS, attachments);

    logicalContext.putAll(httpProperties);
    soapContext.putAll(httpProperties);

    importAppProperties(logicalContext, soapContext, request);

    RuntimeException runtimeException = null;

    for (int i = 0; i < _chain.size(); i++) {
      Handler handler = _chain.get(i);

      boolean success = false;
      _invoked[i] = true;

      try {
        if (handler instanceof LogicalHandler) {
          logicalContext.getMessage().setPayload(source);
          success = handler.handleMessage(logicalContext);
          source = logicalContext.getMessage().getPayload();
        }
        else if (handler instanceof SOAPHandler) {
          try {
            soapContext.setMessage(source);
            success = handler.handleMessage(soapContext);
            source = soapContext.getMessage().getSOAPPart().getContent();
          }
          catch (SOAPException e) {
            throw new WebServiceException(e);
          }
        }
        else {
          throw new WebServiceException(L.l("Unsupported Handler type: {0}",
                                            handler.getClass().getName()));
        }

        if (! success)
          break;
      }
      catch (RuntimeException e) {
        runtimeException = e;
        break;
      }
    }

    // close in reverse order, only closing the handlers that were 
    // actually invoked
    if (close) {
      for (int i = _chain.size() - 1; i >= 0; i--) {
        Handler handler = _chain.get(i);

        if (! _invoked[i])
          continue;

        _invoked[i] = false;

        if (handler instanceof LogicalHandler) {
          logicalContext.getMessage().setPayload(source);
          handler.close(logicalContext);
          source = logicalContext.getMessage().getPayload();
        }
        else if (handler instanceof SOAPHandler) {
          try {
            soapContext.setMessage(source);
            handler.close(soapContext);
            source = soapContext.getMessage().getSOAPPart().getContent();
          }
          catch (SOAPException e) {
            throw new WebServiceException(e);
          }
        }
      }
    }

    exportAppProperties(logicalContext, soapContext, request);

    if (runtimeException != null)
      throw runtimeException;

    try {
      getTransformer().transform(source, new StreamResult(out));
    }
    catch (TransformerException e) {
      throw new WebServiceException(e);
    }
  }

  /**
   * Invoke the handler chain for an inbound message.
   **/
  public InputStream invoke(InputStream in, 
                            Map<String,DataHandler> attachments,
                            Map<String,Object> httpProperties,
                            boolean close, boolean request)
    throws WebServiceException
  {
    Source source = null;

    try {
      DOMResult dom = new DOMResult();
      getTransformer().transform(new StreamSource(in), dom);

      // XXX The TCK seems to assume a source that will stand up to repeated
      // reads... meaning that StreamSource and SAXSource are out, so DOM
      // must be what they want.
      source = new DOMSource(dom.getNode());
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }

    LogicalMessageContextImpl logicalContext = new LogicalMessageContextImpl();
    SOAPMessageContextImpl soapContext = new SOAPMessageContextImpl();

    // Set the mandatory properties
    logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);
    soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);

    logicalContext.put(INBOUND_MESSAGE_ATTACHMENTS, attachments);
    soapContext.put(INBOUND_MESSAGE_ATTACHMENTS, attachments);

    logicalContext.putAll(httpProperties);
    soapContext.putAll(httpProperties);

    importAppProperties(logicalContext, soapContext, request);

    RuntimeException runtimeException = null;

    // NOTE: the order is reversed for inbound messages
    for (int i = _chain.size() - 1; i >= 0; i--) {
      Handler handler = _chain.get(i);

      boolean success = false;
      _invoked[i] = true;

      try {
        if (handler instanceof LogicalHandler) {
          logicalContext.getMessage().setPayload(source);
          success = handler.handleMessage(logicalContext);
          source = logicalContext.getMessage().getPayload();
        }
        else if (handler instanceof SOAPHandler) {
          try {
            soapContext.setMessage(source);
            success = handler.handleMessage(soapContext);
            source = soapContext.getMessage().getSOAPPart().getContent();
          }
          catch (SOAPException e) {
            throw new WebServiceException(e);
          }
        }
        else {
          throw new WebServiceException(L.l("Unsupported Handler type: {0}",
                                            handler.getClass().getName()));
        }
      }
      catch (RuntimeException e) {
        runtimeException = e;
        break;
      }

      if (! success)
        break;
    }

    if (close) {
      // close in reverse order, only closing the handlers that were 
      // actually invoked
      for (int i = _chain.size() - 1; i >= 0; i--) {
        Handler handler = _chain.get(i);

        if (! _invoked[i])
          continue;

        _invoked[i] = false;

        if (handler instanceof LogicalHandler) {
          logicalContext.getMessage().setPayload(source);
          handler.close(logicalContext);
          source = logicalContext.getMessage().getPayload();
        }
        else if (handler instanceof SOAPHandler) {
          try {
            soapContext.setMessage(source);
            handler.close(soapContext);
            source = soapContext.getMessage().getSOAPPart().getContent();
          }
          catch (SOAPException e) {
            throw new WebServiceException(e);
          }
        }
      }
    }

    exportAppProperties(logicalContext, soapContext, request);

    if (runtimeException != null)
      throw runtimeException;

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      getTransformer().transform(source, new StreamResult(baos));

      return new ByteArrayInputStream(baos.toByteArray());
    }
    catch (TransformerException e) {
      throw new WebServiceException(e);
    }
  }

  private void importAppProperties(LogicalMessageContextImpl logicalContext,
                                   SOAPMessageContextImpl soapContext,
                                   boolean request)
  {
    if (request) {
      logicalContext.putAll(_bindingProvider.getRequestContext(), 
                            Scope.APPLICATION);
      soapContext.putAll(_bindingProvider.getRequestContext(),  
                         Scope.APPLICATION);
    }
    else {
      logicalContext.putAll(_bindingProvider.getResponseContext(), 
                            Scope.APPLICATION);
      soapContext.putAll(_bindingProvider.getResponseContext(),  
                         Scope.APPLICATION);
    }
  }

  private void exportAppProperties(LogicalMessageContextImpl logicalContext,
                                   SOAPMessageContextImpl soapContext,
                                   boolean request)
  {
    if (request) { 
      Map<String,Object> map = null;
      
      map = logicalContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getRequestContext().putAll(map);

      map = soapContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getRequestContext().putAll(map);
    }
    else {
      Map<String,Object> map = null;
      
      map = logicalContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getResponseContext().putAll(map);

      map = soapContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getResponseContext().putAll(map);
    }
  }

  private static Transformer getTransformer()
    throws TransformerException
  {
    if (_transformer == null) {
      TransformerFactory factory = TransformerFactory.newInstance();
      _transformer = factory.newTransformer();
    }

    return _transformer;
  }
}
