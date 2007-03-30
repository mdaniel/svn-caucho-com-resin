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
import com.caucho.util.L10N;

import javax.activation.DataSource;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
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

  private final Map<QName,Port> _portMap = new HashMap<QName,Port>();

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
    _portMap.put(portName, new Port(bindingId, endpointAddress));
  }

  public <T> Dispatch<T> createDispatch(QName portName,
                                        Class<T> type,
                                        Service.Mode mode)
    throws WebServiceException
  {
    Port port = _portMap.get(portName);

    if (port == null)
      throw new WebServiceException(L.l("{0} is an unknown port", portName));

    if (Source.class.equals(type)) {
      return (Dispatch<T>) new SourceDispatch(port.getBindingId(),
                                              port.getEndpointAddress(),
                                              mode, _executor);
    }
    else if (SOAPMessage.class.equals(type)) {
      return (Dispatch<T>) new SOAPMessageDispatch(port.getBindingId(),
                                                   port.getEndpointAddress(),
                                                   mode, _executor);
    }
    else if (DataSource.class.equals(type)) {
      return (Dispatch<T>) new DataSourceDispatch(port.getBindingId(),
                                                  port.getEndpointAddress(),
                                                  mode, _executor);
    }

    throw new WebServiceException(L.l("{0} is an unsupported Dispatch type",
                                      type));
  }

  public Dispatch<Object> createDispatch(QName portName,
					                               JAXBContext context,
                                         Service.Mode mode)
    throws WebServiceException
  {
    Port port = _portMap.get(portName);

    if (port == null)
      throw new WebServiceException(L.l("{0} is an unknown port", portName));

    return new JAXBDispatch(port.getBindingId(), port.getEndpointAddress(),
                            mode, _executor, context);
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
  {
    try {
      Skeleton skeleton = new WebServiceIntrospector().introspect(api, null);

      PortProxyHandler handler = new PortProxyHandler(skeleton);

      return (T) Proxy.newProxyInstance(_classLoader,
					                              new Class[] { api, 
                                                      BindingProvider.class },
                                        handler);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      // XXX:
      throw new RuntimeException(e);
    }
  }

  public Iterator<QName> getPorts()
  {
    throw new UnsupportedOperationException();
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

  private static class Port {
    private final String _bindingId;
    private final String _endpointAddress;

    public Port(String bindingId, String endpointAddress)
    {
      _bindingId = bindingId;
      _endpointAddress = endpointAddress;
    }

    public String getBindingId()
    {
      return _bindingId;
    }

    public String getEndpointAddress()
    {
      return _endpointAddress;
    }
  }
}
