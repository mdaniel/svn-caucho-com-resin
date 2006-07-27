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

/** XXX */
public class Service {

  protected Service(URL wsdlDocumentLocation, QName serviceName)
  {
  }


  /** XXX */
  public void addPort(QName portName, String bindingId, String endpointAddress)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
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


  /** XXX */
  public static Service create(URL wsdlDocumentLocation, QName serviceName)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <T> Dispatch<T> createDispatch(QName portName, Class<T> type,
                                        Mode mode)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
    /*
  public Dispatch<Object> createDispatch(QName portName,
                                         JAXBContext context, Mode mode)
  {
    throw new UnsupportedOperationException();
  }

  /** XXX */
  public Executor getExecutor()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public HandlerResolver getHandlerResolver()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <T> T getPort(Class<T> serviceEndpointName)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public <T> T getPort(QName portName, Class<T> serviceEndpointName)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Iterator<QName> getPorts()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public QName getServiceName()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public URL getWSDLDocumentLocation()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setExecutor(Executor executor)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setHandlerResolver(HandlerResolver handlerResolver)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public static enum Mode {

      MESSAGE, PAYLOAD;

  }
}

