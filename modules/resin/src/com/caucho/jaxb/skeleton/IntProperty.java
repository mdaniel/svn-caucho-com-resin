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
 * @author Adam Megacz
 */

package com.caucho.jaxb.skeleton;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

/**
 * a Int Property
 */
public class IntProperty extends CDataProperty {
  public static final IntProperty OBJECT_PROPERTY = new IntProperty(true);
  public static final IntProperty PRIMITIVE_PROPERTY = new IntProperty(false);

  protected IntProperty(boolean isNillable)
  {
    _isNillable = isNillable;
  }

  protected String write(Object in)
  {
    return DatatypeConverter.printInt(((Integer) in).intValue());
  }

  protected Object read(String in)
  {
    return DatatypeConverter.parseInt(in);
  }

  public String getSchemaType()
  {
    return "xsd:int";
  }

  public void write(Marshaller m, XMLStreamWriter out, int i, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    writeQNameStartElement(out, qname);
    out.writeCharacters(DatatypeConverter.printInt(i));
    writeQNameEndElement(out, qname);
  }
}


