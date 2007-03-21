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

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Behaves like a DocumentWrappedAction most of the time... except for the WSDL?
 */
public class RpcAction extends DocumentWrappedAction {
  private final static Logger log = Logger.getLogger(RpcAction.class.getName());
  public static final L10N L = new L10N(RpcAction.class);

  public RpcAction(Method method, Method eiMethod,
                   JAXBContextImpl jaxbContext, 
                   String targetNamespace,
                   Marshaller marshaller,
                   Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    super(method, eiMethod, 
          jaxbContext, targetNamespace, 
          marshaller, unmarshaller);
  }
  
}
