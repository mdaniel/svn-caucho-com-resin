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
 * @author Scott Ferguson
 */

package com.caucho.soap.jaxws;

import com.caucho.server.util.ScheduledThreadPool;
import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.skeleton.Skeleton;
import com.caucho.soap.wsdl.WSDLDefinitions;
import com.caucho.soap.wsdl.WSDLParser;
import com.caucho.util.L10N;

import javax.activation.DataSource;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import static javax.xml.soap.SOAPConstants.*;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.spi.ServiceDelegate;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * ServiceDelegate
 */
public class ServiceDelegateImpl extends ServiceDelegate {
  private final static Logger log
    = Logger.getLogger(ServiceDelegateImpl.class.getName());
  private final static L10N L = new L10N(ServiceDelegateImpl.class);

  private final Map<QName,PortInfoImpl> _portMap 
    = new HashMap<QName,PortInfoImpl>();

  private final ClassLoader _classLoader;

  private final URL _wsdl;
  private final QName _serviceName;
  private final Class _serviceClass;

  private Executor _executor = ScheduledThreadPool.getLocal();

  ServiceDelegateImpl(URL wsdl, QName serviceName, Class serviceClass)
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
    
    _wsdl = wsdl;
    _serviceName = serviceName;
    _serviceClass = serviceClass;
  }

  public void addPort(QName portName, String bindingId, String endpointAddress)
  {
    PortInfoImpl portInfo = 
      new PortInfoImpl(bindingId, portName, _serviceName, endpointAddress);

    _portMap.put(portName, portInfo);
  }

  public <T> Dispatch<T> createDispatch(QName portName,
                                        Class<T> type,
                                        Service.Mode mode)
    throws WebServiceException
  {
    PortInfoImpl port = _portMap.get(portName);
    String bindingId = URI_NS_SOAP_ENVELOPE;
    String endpointAddress = null;

    if (port != null) {
      bindingId = port.getBindingID();
      endpointAddress = port.getEndpointAddress();
    }

    if (endpointAddress == null)
      endpointAddress = findEndpointAddress();

    Dispatch<T> dispatch = null;

    if (Source.class.equals(type)) {
      dispatch = (Dispatch<T>) new SourceDispatch(bindingId, mode, _executor);
    }
    else if (SOAPMessage.class.equals(type)) {
      dispatch = 
        (Dispatch<T>) new SOAPMessageDispatch(bindingId, mode, _executor);
    }
    else if (DataSource.class.equals(type)) {
      dispatch = 
        (Dispatch<T>) new DataSourceDispatch(bindingId, mode, _executor);
    }

    if (dispatch == null) {
      throw new WebServiceException(L.l("{0} is an unsupported Dispatch type",
                                        type));
    }

    if (endpointAddress != null) {
      Map<String,Object> requestContext = dispatch.getRequestContext();
      requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                         endpointAddress);
    }

    return dispatch;
  }

  public Dispatch<Object> createDispatch(QName portName,
					                               JAXBContext context,
                                         Service.Mode mode)
    throws WebServiceException
  {
    PortInfoImpl port = _portMap.get(portName);
    String bindingId = URI_NS_SOAP_ENVELOPE;
    String endpointAddress = null;

    if (port != null) {
      bindingId = port.getBindingID();
      endpointAddress = port.getEndpointAddress();
    }

    if (endpointAddress == null)
      endpointAddress = findEndpointAddress();

    JAXBDispatch dispatch = 
      new JAXBDispatch(bindingId, mode, _executor, context);

    if (endpointAddress != null) {
      Map<String,Object> requestContext = dispatch.getRequestContext();
      requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                         endpointAddress);
    }

    return dispatch;
  }

  public Executor getExecutor()
  {
    return _executor;
  }

  public HandlerResolver getHandlerResolver()
  {
    throw new UnsupportedOperationException();
  }

  public <T> T getPort(Class<T> serviceEndpointInterface)
  {
    return getPort(null, serviceEndpointInterface);
  }

  public <T> T getPort(QName portName, Class<T> api)
    throws WebServiceException
  {
    try {
      Skeleton skeleton = new WebServiceIntrospector().introspect(api, null);

      PortProxyHandler handler = new PortProxyHandler(skeleton);

      WSDLDefinitions definitions = WSDLParser.parse(api);
      String endpointAddress = findEndpointAddress();

      // XXX bindingId

      PortInfoImpl portInfo = 
        new PortInfoImpl(null, portName, _serviceName, endpointAddress);

      _portMap.put(portName, portInfo);

      return (T) Proxy.newProxyInstance(_classLoader,
					                              new Class[] { api, 
                                                      BindingProvider.class },
                                        handler);
    }
    catch (WebServiceException e) {
      throw e;
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }
  }

  public Iterator<QName> getPorts()
  {
    return _portMap.keySet().iterator();
  }

  public QName getServiceName()
  {
    return _serviceName;
  }

  public URL getWSDLDocumentLocation()
  {
    return _wsdl;
  }

  public void setExecutor(Executor executor)
  {
    _executor = executor;
  }

  public void setHandlerResolver(HandlerResolver handlerResolver)
  {
  }

  public String toString()
  {
    return ("ServiceDelegateImpl[" + getServiceName().getNamespaceURI()
	    + "," + getServiceName().getLocalPart() + "]");
  }

  private String findEndpointAddress()
  {
    if (getWSDLDocumentLocation() != null) {
      int p = getWSDLDocumentLocation().toString().lastIndexOf('?');
      return getWSDLDocumentLocation().toString().substring(0, p);
    }
    else
      return null;

    /*
    WSDLDefinitions definitions = WSDLParser.parse(getWSDLDocumentLocation());

    if (definitions != null) {
      endpointAddress = 
        definitions.getEndpointAddress(_serviceName, portName);

      if (endpointAddress != null && endpointAddress.indexOf(':') < 0) {
        definitions = WSDLParser.parse(endpointAddress);

        if (definitions != null) {
          endpointAddress = 
            definitions.getEndpointAddress(_serviceName, portName);
        }
      }
    }*/
  }
}
