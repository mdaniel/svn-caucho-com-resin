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

package javax.xml.bind;
import java.io.*;
import org.w3c.dom.*;
import java.util.*;
import java.lang.reflect.*;

public abstract class JAXBContext {

  public static final String JAXB_CONTEXT_FACTORY =
    "javax.xml.bind.context.factory";

  protected JAXBContext()
  {
  }


  public Binder<Node> createBinder()
  {
    throw new UnsupportedOperationException();
  }


  public <T> Binder<T> createBinder(Class<T> domType)
  {
    throw new UnsupportedOperationException();
  }

  
  public JAXBIntrospector createJAXBIntrospector()
  {
    throw new UnsupportedOperationException();
  }

  public abstract Marshaller createMarshaller() throws JAXBException;

  public abstract Unmarshaller createUnmarshaller() throws JAXBException;

  public Validator createValidator() throws JAXBException
  {
    throw new UnsupportedOperationException("javax.xml.bind.Validator was removed in JAXB 2.0");
  }
  
  public void generateSchema(SchemaOutputResolver outputResolver) throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public static JAXBContext newInstance(Class... classesToBeBound)
      throws JAXBException
  {
    return newInstance(classesToBeBound,
		       null);
  }

  public static JAXBContext newInstance(Class[] classesToBeBound,
                                        Map<String,?> properties)
      throws JAXBException
  {
    try {
      Class c = Class.forName("com.caucho.jaxb.JAXBContextImpl");
      Constructor con = c.getConstructor(new Class[] {
	Class[].class,
	Map.class
      });
      return (JAXBContext)
	con.newInstance(new Object[] {
	  classesToBeBound,
	  properties
	});
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public static JAXBContext newInstance(String contextPath) throws JAXBException
  {
    return newInstance(contextPath,
		       Thread.currentThread().getContextClassLoader(),
		       null);
  }

  public static JAXBContext newInstance(String contextPath,
					ClassLoader classLoader)
    throws JAXBException
  {
    return newInstance(contextPath, classLoader, null);
  }

  public static JAXBContext newInstance(String contextPath,
                                        ClassLoader classLoader,
                                        Map<String,?> properties)
      throws JAXBException
  {
    try {
      Class c = Class.forName("com.caucho.jaxb.JAXBContextImpl");
      Constructor con = c.getConstructor(new Class[] {
	String.class,
	ClassLoader.class,
	Map.class
      });
      return (JAXBContext)
	con.newInstance(new Object[] {
	  contextPath,
	  classLoader,
	  properties
	});
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

}

