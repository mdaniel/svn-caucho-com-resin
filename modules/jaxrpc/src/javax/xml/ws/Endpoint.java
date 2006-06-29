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
import javax.xml.transform.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A Web service endpoint. Endpoints are created using the static methods
 * defined in this class. An endpoint is always tied to one Binding and one
 * implementor, both set at endpoint creation time. An endpoint is either in a
 * published or an unpublished state. The publish methods can be used to start
 * publishing an endpoint, at which point it starts accepting incoming
 * requests. Conversely, the stop method can be used to stop accepting incoming
 * requests and take the endpoint down. Once stopped, an endpoint cannot be
 * published again. An Executor may be set on the endpoint in order to gain
 * better control over the threads used to dispatch incoming requests. For
 * instance, thread pooling with certain parameters can be enabled by creating
 * a ThreadPoolExecutor and registering it with the endpoint. Handler chains
 * can be set using the contained Binding. An endpoint may have a list of
 * metadata documents, such as WSDL and XMLSchema documents, bound to it. At
 * publishing time, the JAX-WS implementation will try to reuse as much of that
 * metadata as possible instead of generating new one based on the annotations
 * present on the implementor. Since: JAX-WS 2.0 See Also:Binding, BindingType,
 * SOAPBinding, Executor
 */
public abstract class Endpoint {

  /**
   * Standard property: name of WSDL port. Type: javax.xml.namespace.QName See
   * Also:Constant Field Values
   */
  public static final String WSDL_PORT="javax.xml.ws.wsdl.port";


  /**
   * Standard property: name of WSDL service. Type: javax.xml.namespace.QName
   * See Also:Constant Field Values
   */
  public static final String WSDL_SERVICE="javax.xml.ws.wsdl.service";

  public Endpoint()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates an endpoint with the specified implementor object. If there is a
   * binding specified via a BindingType annotation then it MUST be used else a
   * default of SOAP 1.1 / HTTP binding MUST be used. The newly created
   * endpoint may be published by calling one of the
   * javax.xml.ws.Endpoint#publish(String) and
   * javax.xml.ws.Endpoint#publish(Object) methods.
   */
  public static Endpoint create(Object implementor)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates an endpoint with the specified binding type and implementor
   * object. The newly created endpoint may be published by calling one of the
   * javax.xml.ws.Endpoint#publish(String) and
   * javax.xml.ws.Endpoint#publish(Object) methods.
   */
  public static Endpoint create(String bindingId, Object implementor)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns the binding for this endpoint.
   */
  public abstract Binding getBinding();


  /**
   * Returns the executor for this Endpointinstance. The executor is used to
   * dispatch an incoming request to the implementor object.
   */
  public abstract Executor getExecutor();


  /**
   * Returns the implementation object for this endpoint.
   */
  public abstract Object getImplementor();


  /**
   * Returns a list of metadata documents for the service.
   */
  public abstract List<Source> getMetadata();


  /**
   * Returns the property bag for this Endpoint instance.
   */
  public abstract Map<String,Object> getProperties();


  /**
   * Returns true if the endpoint is in the published state.
   */
  public abstract boolean isPublished();


  /**
   * Publishes this endpoint at the provided server context. A server context
   * encapsulates the server infrastructure and addressing information for a
   * particular transport. For a call to this method to succeed, the server
   * context passed as an argument to it must be compatible with the endpoint's
   * binding.
   */
  public abstract void publish(Object serverContext);


  /**
   * Publishes this endpoint at the given address. The necessary server
   * infrastructure will be created and configured by the JAX-WS implementation
   * using some default configuration. In order to get more control over the
   * server configuration, please use the javax.xml.ws.Endpoint#publish(Object)
   * method instead.
   */
  public abstract void publish(String address);


  /**
   * Creates and publishes an endpoint for the specified implementor object at
   * the given address. The necessary server infrastructure will be created and
   * configured by the JAX-WS implementation using some default configuration.
   * In order to get more control over the server configuration, please use the
   * javax.xml.ws.Endpoint#create(String,Object) and
   * javax.xml.ws.Endpoint#publish(Object) method instead.
   */
  public static Endpoint publish(String address, Object implementor)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the executor for this Endpoint instance. The executor is used to
   * dispatch an incoming request to the implementor object. If this Endpoint
   * is published using the publish(Object) method and the specified server
   * context defines its own threading behavior, the executor may be ignored.
   */
  public abstract void setExecutor(Executor executor);


  /**
   * Sets the metadata for this endpoint.
   */
  public abstract void setMetadata(List<Source> metadata);


  /**
   * Sets the property bag for this Endpoint instance.
   */
  public abstract void setProperties(Map<String,Object> properties);


  /**
   * Stops publishing this endpoint. If the endpoint is not in a published
   * state, this method has not effect.
   */
  public abstract void stop();

}

