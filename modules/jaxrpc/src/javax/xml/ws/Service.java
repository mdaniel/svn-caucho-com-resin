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

package javax.xml.ws;
import javax.xml.namespace.*;
import javax.xml.ws.handler.*;
import javax.xml.ws.Service.*;
import javax.xml.bind.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Service objects provide the client view of a Web service. Service acts as a
 * factory of the following: Proxies for a target service endpoint. Instances
 * of javax.xml.ws.Dispatch for dynamic message-oriented invocation of a remote
 * operation. The ports available on a service can be enumerated using the
 * getPorts method. Alternatively, you can pass a service endpoint interface to
 * the unary getPort method and let the runtime select a compatible port.
 * Handler chains for all the objects created by a Service can be set by means
 * of a HandlerResolver. An Executor may be set on the service in order to gain
 * better control over the threads used to dispatch asynchronous callbacks. For
 * instance, thread pooling with certain parameters can be enabled by creating
 * a ThreadPoolExecutor and registering it with the service. Since: JAX-WS 2.0
 * See Also:Provider, HandlerResolver, Executor
 */
public class Service {

  protected Service(URL wsdlDocumentLocation, QName serviceName)
  {
  }


  /**
   * Creates a new port for the service. Ports created in this way contain no
   * WSDL port type information and can only be used for creating
   * Dispatchinstances.
   */
  public void addPort(QName portName, String bindingId, String endpointAddress)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a Service instance.
   */
  public static Service create(QName serviceName)
  {
    try {
      Class c = Class.forName("com.caucho.soap.service.ServiceImpl");
      Constructor con = c.getConstructor(new Class[] { QName.class });
      return (Service)con.newInstance(new Object[] { serviceName });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Create a Service instance. The specified WSDL document location and
   * service qualified name must uniquely identify a wsdl:service element.
   */
  public static Service create(URL wsdlDocumentLocation, QName serviceName)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a Dispatch instance for use with objects of the users choosing.
   */
  public <T> Dispatch<T> createDispatch(QName portName, Class<T> type,
                                        Mode mode)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a Dispatch instance for use with JAXB generated objects.
   */
    /*
  public Dispatch<Object> createDispatch(QName portName,
                                         JAXBContext context, Mode mode)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the executor for this Serviceinstance. The executor is used for
   * all asynchronous invocations that require callbacks.
   */
  public Executor getExecutor()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns the configured handler resolver.
   */
  public HandlerResolver getHandlerResolver()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * The getPort method returns a stub. The parameter serviceEndpointInterface
   * specifies the service endpoint interface that is supported by the returned
   * proxy. In the implementation of this method, the JAX-WS runtime system
   * takes the responsibility of selecting a protocol binding (and a port) and
   * configuring the proxy accordingly. The returned proxy should not be
   * reconfigured by the client.
   */
  public <T> T getPort(Class<T> serviceEndpointName)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * The getPort method returns a stub. A service client uses this stub to
   * invoke operations on the target service endpoint. The
   * serviceEndpointInterface specifies the service endpoint interface that is
   * supported by the created dynamic proxy or stub instance.
   */
  public <T> T getPort(QName portName, Class<T> serviceEndpointName)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns an Iterator for the list of QNames of service endpoints grouped by
   * this service
   */
  public Iterator<QName> getPorts()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the name of this service.
   */
  public QName getServiceName()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the location of the WSDL document for this Service.
   */
  public URL getWSDLDocumentLocation()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the executor for this Service instance. The executor is used for all
   * asynchronous invocations that require callbacks.
   */
  public void setExecutor(Executor executor)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the HandlerResolver for this Service instance. The handler resolver,
   * if present, will be called once for each proxy or dispatch instance that
   * is created, and the handler chain returned by the resolver will be set on
   * the instance.
   */
  public void setHandlerResolver(HandlerResolver handlerResolver)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * The orientation of a dynamic client or service. MESSAGE provides access to
   * entire protocol message, PAYLOAD to protocol message payload only.
   */
  public static enum Mode {

      MESSAGE, PAYLOAD;

  }
}

