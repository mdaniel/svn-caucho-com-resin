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

package com.caucho.soap.reflect;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.soap.marshall.MarshallFactory;
import com.caucho.soap.skeleton.DirectSkeleton;
import com.caucho.soap.skeleton.PojoMethodSkeleton;
import com.caucho.util.L10N;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
//import com.caucho.xml.*;

/**
 * Introspects a web service
 */
public class WebServiceIntrospector {
  private static XMLOutputFactory _outputFactory = null;

  public static final L10N L = new L10N(WebServiceIntrospector.class);
  /**
   * Introspects the class
   */
  public DirectSkeleton introspect(Class type)
    //throws ConfigException
    throws JAXBException
  {
    return introspect(type, null);
  }
	   
  /**
   * Introspects the class
   */
  public DirectSkeleton introspect(Class type, String wsdlLocation)
    //throws ConfigException
    throws JAXBException
  {
    // server/4221 vs soap/0301
    /*
    if (! type.isAnnotationPresent(WebService.class))
      throw new RuntimeException(L.l("{0}: needs a @WebService annotation.",
                                     type.getName()));
    */
    
    boolean isInterface = type.isInterface();

    MarshallFactory marshallFactory = new MarshallFactory();

    WebService webService = (WebService) type.getAnnotation(WebService.class);

    // Get all the classes that JAXB needs, then generate schema for them
    HashSet<Class> jaxbClasses = new HashSet<Class>();
    JAXBUtil.introspectClass(type, jaxbClasses);

    JAXBContextImpl jaxbContext = 
      new JAXBContextImpl(jaxbClasses.toArray(new Class[0]), null);

    DirectSkeleton skel = new DirectSkeleton(type, wsdlLocation);
    String namespace = skel.getNamespace();

    LinkedHashMap<String,String> elements = new LinkedHashMap<String,String>();
    Method[] methods = type.getMethods();

    for (int i = 0; i < methods.length; i++) {
      if ((methods[i].getModifiers() & Modifier.PUBLIC) == 0)
        continue;

      WebMethod webMethod = methods[i].getAnnotation(WebMethod.class);

      if (webService == null && webMethod == null && ! isInterface)
        continue;

      if (webMethod == null && methods[i].getDeclaringClass() != type)
        continue;

      // XXX: needs test
      if (webMethod != null && webMethod.exclude())
        continue;

      PojoMethodSkeleton methodSkel = 
        PojoMethodSkeleton.createMethodSkeleton(methods[i], marshallFactory, 
                                                jaxbContext, namespace,
                                                elements);

      String name = webMethod == null ? "" : webMethod.operationName();

      if (name.equals(""))
          name = methods[i].getName();

      skel.addAction(name, methodSkel);
    }

    /*
    Node typesNode = skel.getTypesNode();
    DOMResult result = new DOMResult(typesNode);

    try {
      XMLStreamWriter out = getStreamWriter(result);
      jaxbContext.generateSchemaWithoutHeader(out, elements);
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }*/

    return skel;
  }

  private static XMLStreamWriter getStreamWriter(DOMResult result)
    throws XMLStreamException
  {
    if (_outputFactory == null)
      _outputFactory = XMLOutputFactory.newInstance();

    return _outputFactory.createXMLStreamWriter(result);
  }
}
