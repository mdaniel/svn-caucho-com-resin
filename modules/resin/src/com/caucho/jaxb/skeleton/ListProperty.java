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

import com.caucho.jaxb.JAXBUtil;

import javax.xml.bind.JAXBException;
import java.util.Iterator;
import java.util.List;

/**
 * a List Property
 */
public class ListProperty extends IterableProperty {
  public int size(Object o)
  {
    return ((List) o).size();
  }

  public Iterator getIterator(Object o)
  {
    return ((List) o).iterator();
  }

  public String getSchemaType()
  {
    /*
    Property p = getComponentProperty();
    Accessor a = p.getAccessor();

    return JAXBUtil.getXmlSchemaDatatype(a.getType());*/
    return "xsd:string"; // XXX
  }

  public boolean isXmlPrimitiveType()
  {
    return getComponentProperty().isXmlPrimitiveType();
  }

  public String getMaxOccurs()
  {
    return "unbounded";
  }
}


