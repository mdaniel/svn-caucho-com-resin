/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.jaxb;

import com.caucho.jaxb.skeleton.ClassSkeleton;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Generates various parts of schema.
 */
public class SchemaGenerator {
  private final JAXBContextImpl _context;

  public SchemaGenerator(JAXBContextImpl context)
  {
    _context = context;
  }

  public void generateWrappedType(XMLStreamWriter out,
                                  String wrapperName, Class... wrappedClasses)
    throws XMLStreamException, JAXBException
  {
    out.writeStartElement("xsd", 
                          "complexType",
                          "http://www.w3.org/2001/XMLSchema");
    out.writeAttribute("name", wrapperName);

    out.writeStartElement("xsd", 
                          "sequence", 
                          "http://www.w3.org/2001/XMLSchema");
    
    for (Class wrappedClass : wrappedClasses)
      generateSchemaParticle(out, wrappedClass);

    out.writeEndElement(); // sequence

    out.writeEndElement(); // complexType
  }

  public void generateSchemaParticle(XMLStreamWriter out, Class cl)
    throws XMLStreamException, JAXBException
  {
    ClassSkeleton skeleton = _context.getSkeleton(cl);
    skeleton.generateSchema(out);
  }
}
