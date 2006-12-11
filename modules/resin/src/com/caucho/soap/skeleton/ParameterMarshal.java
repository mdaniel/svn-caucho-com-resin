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

package com.caucho.soap.skeleton;

import com.caucho.jaxb.skeleton.Property;

import javax.jws.WebParam;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

abstract public class ParameterMarshal {
  protected final int _arg;
  protected final Property _property;
  protected final QName _name;
  protected final Marshaller _marshaller;
  protected final Unmarshaller _unmarshaller;

  protected ParameterMarshal(int arg, Property property, QName name, 
                             Marshaller marshaller, Unmarshaller unmarshaller)
  {
    _arg = arg;
    _property = property;
    _name = name;
    _marshaller = marshaller;
    _unmarshaller = unmarshaller;
  }

  static ParameterMarshal create(int arg, 
                                 Property property, 
                                 QName name,
                                 WebParam.Mode mode, 
                                 Marshaller marshaller, 
                                 Unmarshaller unmarshaller)
  {
    switch (mode) {
    case IN:
      return new InParameterMarshal(arg, property, name, 
                                    marshaller, unmarshaller);
    case OUT:
      return new OutParameterMarshal(arg, property, name, 
                                     marshaller, unmarshaller);
    default:
      throw new UnsupportedOperationException();
    }
  }

  public int getArg()
  {
    return _arg;
  }

  //
  // client
  //

  public void serializeCall(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  public Object deserializeReply(XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    return null;
  }
  
  public void deserializeReply(XMLStreamReader in, Object[] args)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  //
  // server
  //

  public void deserializeCall(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  public void deserializeCallDefault(Object []args)
  {
  }

  public void serializeReply(XMLStreamWriter out, Object ret)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  public void serializeReply(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
  }
}
