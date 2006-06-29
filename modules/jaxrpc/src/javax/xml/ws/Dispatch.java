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
import java.util.concurrent.*;

/**
 * The Dispatch interface provides support for the dynamic invocation of a
 * service endpoint operations. The javax.xml.ws.Service interface acts as a
 * factory for the creation of Dispatch instances. Since: JAX-WS 2.0
 */
public interface Dispatch<T> extends BindingProvider {

  /**
   * Invoke a service operation synchronously. The client is responsible for
   * ensuring that the msg object when marshalled is formed according to the
   * requirements of the protocol binding in use.
   */
  abstract T invoke(T msg);


  /**
   * Invoke a service operation asynchronously. The method returns without
   * waiting for the response to the operation invocation, the results of the
   * operation are obtained by polling the returned Response. The client is
   * responsible for ensuring that the msg object when marshalled is formed
   * according to the requirements of the protocol binding in use.
   */
  abstract T invokeAsync(T msg);


  /**
   * Invoke a service operation asynchronously. The method returns without
   * waiting for the response to the operation invocation, the results of the
   * operation are communicated to the client via the passed in handler. The
   * client is responsible for ensuring that the msg object when marshalled is
   * formed according to the requirements of the protocol binding in use.
   */
  abstract Future<T> invokeAsync(Dispatch msg, AsyncHandler<T> async);


  /**
   * Invokes a service operation using the one-way interaction mode. The
   * operation invocation is logically non-blocking, subject to the
   * capabilities of the underlying protocol, no results are returned. When the
   * protocol in use is SOAP/HTTP, this method must block until an HTTP
   * response code has been received or an error occurs. The client is
   * responsible for ensuring that the msg object when marshalled is formed
   * according to the requirements of the protocol binding in use.
   */
  abstract void invokeOneWay(T msg);

}

