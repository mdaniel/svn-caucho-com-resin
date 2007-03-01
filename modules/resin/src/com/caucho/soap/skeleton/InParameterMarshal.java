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
 * @author Scott Ferguson
 */

package com.caucho.soap.skeleton;

import com.caucho.jaxb.skeleton.Property;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

public class InParameterMarshal extends ParameterMarshal {
  public static final L10N L = new L10N(InParameterMarshal.class);

  public InParameterMarshal(int arg, Property property, QName name,
                            Marshaller marshaller, Unmarshaller unmarshaller)
  {
    super(arg, property, name, marshaller, unmarshaller);
  }

  //
  // client
  //

  public void serializeCall(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    _property.write(_marshaller, out, args[_arg], _name);
  }

  //
  // server
  //

  public void deserializeCall(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    args[_arg] = _property.read(_unmarshaller, in, args[_arg]);
  }
}
