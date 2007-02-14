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

package com.caucho.jaxb.skeleton;

import java.io.IOException;

import java.lang.reflect.Field;

import java.util.HashMap;

import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import javax.xml.stream.XMLStreamException;

import com.caucho.util.L10N;

/**
 * a string property
 */
public class EnumProperty<E extends Enum<E>> extends CDataProperty {
  private static final L10N L = new L10N(EnumProperty.class);

  private final Class<E> _enum;
  private final HashMap<String,E> _valueMap = new HashMap<String,E>();
  private final HashMap<E,String> _nameMap = new HashMap<E,String>();

  public EnumProperty(Class<E> e)
    throws JAXBException
  {
    _enum = e;

    try {
      // XXX do something with this value
      XmlEnum xmlEnum = _enum.getAnnotation(XmlEnum.class);

      Field[] fields = _enum.getFields();

      for (int i = 0; i < fields.length; i++) {
        Field f = fields[i];

        // We only care about actual enum fields
        if (! fields[i].isEnumConstant())
          continue;

        XmlEnumValue xmlEnumValue = f.getAnnotation(XmlEnumValue.class);
        E value = Enum.valueOf(_enum, f.getName());

        if (xmlEnumValue != null) {
          _valueMap.put(xmlEnumValue.value(), value);
          _nameMap.put(value, xmlEnumValue.value());
        }
        else {
          _valueMap.put(f.getName(), value);
          _nameMap.put(value, f.getName());
        }
      }
    }
    catch (Exception ex) {
      throw new JAXBException(L.l("Error while introspecting enum {0}", 
                                  e.getName()), ex);
    }
  }

  protected String write(Object in)
    throws JAXBException
  {
    String out = _nameMap.get(in);

    if (out == null)
      throw new MarshalException(L.l("Unknown enum value: {0}", in));

    return out;
  }

  protected Object read(String in)
    throws JAXBException
  {
    Object obj = _valueMap.get(in);

    if (obj == null)
      throw new UnmarshalException(L.l("Invalid enum string: {0}", in));

    return obj;
  }

  public String getSchemaType()
  {
    return "xsd:string";
  }

  // XXX: schema generation
}
