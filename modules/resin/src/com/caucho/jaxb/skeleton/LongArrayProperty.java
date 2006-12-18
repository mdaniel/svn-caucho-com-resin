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
 * @author Emil Ong
 */

package com.caucho.jaxb.skeleton;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

import com.caucho.util.L10N;

/**
 * a property for serializing/deserializing arrays
 */
public class LongArrayProperty extends ArrayProperty {
  private static final L10N L = new L10N(LongArrayProperty.class);

  public static final LongArrayProperty PROPERTY = new LongArrayProperty();

  public LongArrayProperty()
  {
    super(LongProperty.PROPERTY);
  }

  public Object read(Unmarshaller u, XMLStreamReader in, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (in.getEventType() != in.START_ELEMENT || ! qname.equals(in.getName()))
      return new long[0]; // avoid ArrayList instantiation

    ArrayList<Long> ret = new ArrayList<Long>();

    while (in.getEventType() == in.START_ELEMENT && qname.equals(in.getName()))
      ret.add((Long) _componentProperty.read(u, in, qname));

    long[] array = new long[ret.size()];

    for (int i = 0; i < ret.size(); i++)
      array[i] = ret.get(i).longValue();

    return array;
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    //XXX wrapper
    
    if (obj != null) {
      long[] array = (long[]) obj;

      for (int i = 0; i < array.length; i++) 
        LongProperty.PROPERTY.write(m, out, array[i], qname);
    }
  }
}
