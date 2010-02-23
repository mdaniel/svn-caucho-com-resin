/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.webbeans;

import com.caucho.util.Base64;
import com.caucho.util.Crc64;
import com.caucho.util.LruCache;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import javax.enterprise.inject.spi.Bean;

/**
 * Configuration for the xml web bean component.
 */
public class ScopeIdMap
{
  private LruCache<Bean,String> _mapCache = new LruCache<Bean,String>(128);

  public String getId(Bean<?> bean)
  {
    String id = _mapCache.get(bean);

    if (id == null) {
      id = generateScopeId(bean);

      _mapCache.put(bean, id);
    }

    return id;
  }

  private String generateScopeId(Bean<?> bean)
  {
    long crc64 = 17;

    ArrayList<String> types = new ArrayList<String>();
    for (Type type : bean.getTypes()) {
      types.add(String.valueOf(type));
    }

    Collections.sort(types);
    for (String type : types) {
      crc64 = Crc64.generate(crc64, type);
    }

    if (bean.getName() != null)
      crc64 = Crc64.generate(crc64, bean.getName());

    ArrayList<String> qualifiers = new ArrayList<String>();
    for (Annotation qualifier : bean.getQualifiers()) {
      qualifiers.add(String.valueOf(qualifier));
    }

    Collections.sort(qualifiers);
    for (String qualifier : qualifiers) {
      crc64 = Crc64.generate(crc64, qualifier);
    }

    for (String qualifier : qualifiers) {
      crc64 = Crc64.generate(crc64, qualifier);
    }

    StringBuilder sb = new StringBuilder();
    Base64.encode(sb, crc64);

    return sb.toString();
  }
}
