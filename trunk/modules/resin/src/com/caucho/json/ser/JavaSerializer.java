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
 * @author Scott Ferguson
 */

package com.caucho.json.ser;

import com.caucho.json.Json;
import com.caucho.json.JsonOutput;
import com.caucho.json.Transient;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;

public class JavaSerializer extends AbstractJsonSerializer {
  private static final Logger log
    = Logger.getLogger(JavaSerializer.class.getName());

  private Class _type;
  private JsonField []_fields;

  JavaSerializer(Class type, boolean annotated)
  {
    _type = type;

    introspect(annotated);
  }

  void introspect(boolean annotated)
  {
    ArrayList<JsonField> fields = new ArrayList<JsonField>();

    introspectFields(fields, _type, annotated);

    Collections.sort(fields, new JsonFieldComparator());

    _fields = new JsonField[fields.size()];
    fields.toArray(_fields);
  }

  private void introspectFields(ArrayList<JsonField> fields,
                                Class type,
                                boolean annotated)
  {
    if (type == null)
      return;

    introspectFields(fields, type.getSuperclass(), annotated);

    for (Field field : type.getDeclaredFields()) {
      if (Modifier.isTransient(field.getModifiers()))
        continue;
      if (Modifier.isStatic(field.getModifiers()))
        continue;
      if (annotated && field.getAnnotation(Transient.class) != null)
        continue;

      field.setAccessible(true);

      Json json = field.getAnnotation(Json.class);
      JsonField jsonField = new JsonField(field, json);
      fields.add(jsonField);
    }
  }

  public void write(JsonOutput out, Object value, boolean annotated)
    throws IOException
  {
    int i = 0;
    out.writeMapBegin();
    for (JsonField field : _fields) {
      Object fieldValue = null;

      try {
        fieldValue = field.getField().get(value);
      } catch (Exception e) {
        log.warning(out + " cannot get field " + field + " with value " + value);
      }

      if (fieldValue == null)
        continue;

      if (i++ > 0)
        out.writeMapComma();

      out.writeMapEntry(field.getName(), fieldValue, annotated);
    }
    out.writeMapEnd();
  }

  static class JsonField {
    private Field _field;
    private String name;

    JsonField(Field field, Json json)
    {
      _field = field;
      if (json != null)
        name = json.name();
      else
        name = _field.getName();
    }

    public Field getField()
    {
      return _field;
    }

    public String getName()
    {
      return name;
    }
  }

  static class JsonFieldComparator implements Comparator<JsonField> {
    public int compare(JsonField a, JsonField b)
    {
      return a.getName().compareTo(b.getName());
    }
  }
}
