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

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.skeleton.Skeleton;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;
import com.caucho.xml.stream.StaxUtil;
import com.caucho.xml.stream.XMLStreamReaderImpl;
import com.caucho.xml.stream.XMLStreamWriterImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import static javax.xml.soap.SOAPConstants.*;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.spi.ServiceDelegate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.lang.reflect.Proxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Dispatch
 */
public abstract class AbstractDispatch<T> implements Dispatch<T> {
  private final static L10N L = new L10N(AbstractDispatch.class);

  protected final String _bindingId;
  protected final URL _url;
  protected final Service.Mode _mode;
  protected final Executor _executor;

  public AbstractDispatch(String bindingId, String endpointAddress,
                          Service.Mode mode, Executor executor)
    throws WebServiceException
  {
    if (! URI_NS_SOAP_1_2_ENVELOPE.equals(bindingId) &&
        ! URI_NS_SOAP_1_1_ENVELOPE.equals(bindingId))
      throw new WebServiceException(L.l("{0} is not a supported binding id", 
                                        bindingId));

    _bindingId = bindingId;
    _mode = mode;

    try {
      _url = new URL(endpointAddress);
    }
    catch (MalformedURLException e) {
      throw new WebServiceException(e);
    }

    _executor = executor;
  }

  //
  // javax.xml.ws.Dispatch
  //

  public T invoke(T msg)
    throws WebServiceException
  {
    ResponseImpl<T> response = new ResponseImpl<T>();

    invokeNow(msg, response);

    try {
      return response.get();
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }
  }

  public Response<T> invokeAsync(T msg)
  {
    ResponseImpl<T> response = new ResponseImpl<T>();

    _executor.execute(new AsyncInvoker(msg, response));

    return response;
  }

  public Future<?> invokeAsync(T msg, AsyncHandler<T> handler)
  {
    ResponseImpl<T> response = new ResponseImpl<T>();

    _executor.execute(new AsyncInvoker(msg, response, handler));

    return response;
  }

  public void invokeOneWay(T msg)
    throws WebServiceException
  {
    invokeNow(msg, null);
  }

  //
  // javax.xml.ws.BindingProvider
  //

  public Binding getBinding()
  {
    return null;
  }

  public Map<String,Object> getRequestContext()
  {
    return null;
  }

  public Map<String,Object> getResponseContext()
  {
    return null;
  }

  protected abstract void writeRequest(T msg, OutputStream out)
    throws WebServiceException;

  protected abstract T formatResponse(byte[] response)
    throws WebServiceException;

  private void invokeNow(T msg, ResponseImpl<T> response)
    throws WebServiceException
  {
    InputStream in = null;
    OutputStream out = null;

    try {
      URLConnection connection = _url.openConnection();

      if (response != null)
        connection.setDoInput(true);

      connection.setDoOutput(true);

      // send request
      out = connection.getOutputStream();
      
      OutputStreamWriter writer = null;

      if (_mode == Service.Mode.PAYLOAD) {
        writer = new OutputStreamWriter(out);

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.write("<soapenv:Envelope xmlns:soapenv=\"" + _bindingId + "\">");
        writer.write("<soapenv:Body>");

        writer.flush();
      }

      writeRequest(msg, out);

      if (_mode == Service.Mode.PAYLOAD) {
        writer.write("</soapenv:Body>");
        writer.write("</soapenv:Envelope>");

        writer.flush();
      }

      out.flush();

      // read response
      in = connection.getInputStream();

      // XXX for some reason, it seems this is necessary to force the output
      if (response == null) {
        while (in.read() >= 0) {}

        return;
      }

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      if (_mode == Service.Mode.PAYLOAD) {
        XMLStreamReaderImpl reader = new XMLStreamReaderImpl(in);
        
        // skip the Envelope
        reader.nextTag();

        if (reader.getEventType() != reader.START_ELEMENT ||
            ! _bindingId.equals(reader.getNamespaceURI()) ||
            ! "Envelope".equals(reader.getLocalName())) {
          throw new WebServiceException(L.l("Invalid response from server: No Envelope found"));
        }

        // find the body
        
        boolean foundBody = false;

        while (reader.hasNext()) {
          reader.next();

          if (reader.getEventType() == reader.START_ELEMENT &&
              _bindingId.equals(reader.getNamespaceURI()) &&
              "Body".equals(reader.getLocalName())) {

            // Copy the body contents to a StreamDataHandler
            reader.nextTag();

            XMLStreamWriterImpl xmlWriter = 
              new XMLStreamWriterImpl(buffer, false);

            StaxUtil.copyReaderToWriter(reader, xmlWriter);

            xmlWriter.flush();

            foundBody = true;

            break;
          }
        }

        if (! foundBody)
          throw new WebServiceException(L.l("Invalid response from server"));
      }
      else {
        // XXX is a copy necessary here or should we expect the client to
        // close the InputStream?
        int ch = -1;

        while ((ch = in.read()) != -1)
          buffer.write(ch);
      }

      response.set(formatResponse(buffer.toByteArray()));
      response.setContext(connection.getHeaderFields());
    }
    catch (WebServiceException e) {
      throw e;
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }
    finally {
      try {
        if (out != null)
          out.close();

        if (in != null)
          in.close();
      }
      catch (IOException e) {
        throw new WebServiceException(e);
      }
    }
  }

  protected class AsyncInvoker implements Runnable
  {
    private final T _msg;
    private final ResponseImpl<T> _response;
    private final AsyncHandler<T> _handler;

    public AsyncInvoker(T msg, ResponseImpl<T> response)
    {
      this(msg, response, null);
    }

    public AsyncInvoker(T msg, ResponseImpl<T> response,
                        AsyncHandler<T> handler)
    {
      _msg = msg;
      _response = response;
      _handler = handler;
    }

    public void run()
    {
      try {
        invokeNow(_msg, _response);
      }
      catch (Exception e) {
        // XXX do something w/ this exception by setting something in
        // _response
      }

      if (_handler != null)
        _handler.handleResponse(_response);
    }
  }
}
