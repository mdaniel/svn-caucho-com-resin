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

package javax.xml.ws.spi;
import javax.xml.namespace.*;
import javax.xml.ws.*;
import javax.xml.ws.handler.*;
import javax.xml.ws.Service.*;
import javax.xml.bind.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

/**
 * Service delegates are used internally by Service objects to allow
 * pluggability of JAX-WS implementations. Every Service object has its own
 * delegate, created using the javax.xml.ws.Provider#createServiceDelegate
 * method. A Service object delegates all of its instance methods to its
 * delegate. Since: JAX-WS 2.0 See Also:Service, Provider
 */
public abstract class ServiceDelegate {
  protected ServiceDelegate()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a new port for the service. Ports created in this way contain no
   * WSDL port type information and can only be used for creating
   * Dispatchinstances.
   */
  public abstract void addPort(QName portName, String bindingId, String endpointAddress);


  /**
   * Creates a Dispatch instance for use with objects of the users choosing.
   */
  public abstract <T> Dispatch<T> createDispatch(QName portName, Class<T> type, Mode mode);


  /**
   * Creates a Dispatch instance for use with JAXB generated objects.
   */
  public abstract Dispatch<Object> createDispatch(QName portName, JAXBContext context, Mode mode);

  /**
   * Returns the executor for this Serviceinstance. The executor is used for
   * all asynchronous invocations that require callbacks.
   */
  public abstract Executor getExecutor();


  /**
   * Returns the configured handler resolver.
   */
  public abstract HandlerResolver getHandlerResolver();


  /**
   * The getPort method returns a stub. The parameter serviceEndpointInterface
   * specifies the service endpoint interface that is supported by the returned
   * proxy. In the implementation of this method, the JAX-WS runtime system
   * takes the responsibility of selecting a protocol binding (and a port) and
   * configuring the proxy accordingly. The returned proxy should not be
   * reconfigured by the client.
   */
  public abstract <T> T getPort(Class<T> serviceEndpointInterface);


  /**
   * The getPort method returns a stub. A service client uses this stub to
   * invoke operations on the target service endpoint. The
   * serviceEndpointInterface specifies the service endpoint interface that is
   * supported by the created dynamic proxy or stub instance.
   */
  public abstract <T> T getPort(QName portName, Class<T> serviceEndpointInterface);


  /**
   * Returns an Iterator for the list of QNames of service endpoints grouped by
   * this service
   */
  public abstract Iterator<QName> getPorts();


  /**
   * Gets the name of this service.
   */
  public abstract QName getServiceName();


  /**
   * Gets the location of the WSDL document for this Service.
   */
  public abstract URL getWSDLDocumentLocation();


  /**
   * Sets the executor for this Service instance. The executor is used for all
   * asynchronous invocations that require callbacks.
   */
  public abstract void setExecutor(Executor executor);


  /**
   * Sets the HandlerResolver for this Service instance. The handler resolver,
   * if present, will be called once for each proxy or dispatch instance that
   * is created, and the handler chain returned by the resolver will be set on
   * the instance.
   */
  public abstract void setHandlerResolver(HandlerResolver handlerResolver);

}

