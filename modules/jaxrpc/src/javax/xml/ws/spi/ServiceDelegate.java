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

/** XXX */
public abstract class ServiceDelegate {
  protected ServiceDelegate()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public abstract void addPort(QName portName, String bindingId, String endpointAddress);


  /** XXX */
  public abstract <T> Dispatch<T> createDispatch(QName portName, Class<T> type, Mode mode);


  /** XXX */
  public abstract Dispatch<Object> createDispatch(QName portName, JAXBContext context, Mode mode);

  /** XXX */
  public abstract Executor getExecutor();


  /** XXX */
  public abstract HandlerResolver getHandlerResolver();


  /** XXX */
  public abstract <T> T getPort(Class<T> serviceEndpointInterface);


  /** XXX */
  public abstract <T> T getPort(QName portName, Class<T> serviceEndpointInterface);


  /** XXX */
  public abstract Iterator<QName> getPorts();


  /** XXX */
  public abstract QName getServiceName();


  /** XXX */
  public abstract URL getWSDLDocumentLocation();


  /** XXX */
  public abstract void setExecutor(Executor executor);


  /** XXX */
  public abstract void setHandlerResolver(HandlerResolver handlerResolver);

}

