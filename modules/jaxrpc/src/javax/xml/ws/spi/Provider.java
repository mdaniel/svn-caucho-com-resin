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
import java.net.*;
import javax.xml.ws.*;

/**
 * Service provider for ServiceDelegate and Endpoint objects. Since: JAX-WS 2.0
 */
public abstract class Provider {

  /**
   * A constant representing the property used to lookup the name of a Provider
   * implementation class. See Also:Constant Field Values
   */
  public static final String JAXWSPROVIDER_PROPERTY="javax.xml.ws.spi.Provider";


  /**
   * Creates a new instance of Provider
   */
  protected Provider()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates and publishes an endpoint object with the specified address and
   * implementation object.
   */
  public abstract Endpoint createAndPublishEndpoint(String address, Object implementor);


  /**
   * Creates an endpoint object with the provided binding and implementation
   * object.
   */
  public abstract Endpoint createEndpoint(String bindingId, Object implementor);


  /**
   * Creates a service delegate object.
   */
  public abstract ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class serviceClass);


  /**
   * Creates a new provider object. The algorithm used to locate the provider
   * subclass to use consists of the following steps: If a resource with the
   * name of META-INF/services/javax.xml.ws.spi.Provider exists, then its first
   * line, if present, is used as the UTF-8 encoded name of the implementation
   * class. If the $java.home/lib/jaxws.properties file exists and it is
   * readable by the java.util.Properties.load(InputStream) method and it
   * contains an entry whose key is javax.xml.ws.spi.Provider, then the value
   * of that entry is used as the name of the implementation class. If a system
   * property with the name javax.xml.ws.spi.Provider is defined, then its
   * value is used as the name of the implementation class. Finally, a default
   * implementation class name is used.
   */
  public static Provider provider()
  {
    throw new UnsupportedOperationException();
  }

}

