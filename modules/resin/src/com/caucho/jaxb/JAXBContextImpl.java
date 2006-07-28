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

package com.caucho.jaxb;

import javax.xml.bind.*;
import java.util.*;

/**
 * Entry point to API
 */
public class JAXBContextImpl extends JAXBContext {

  private Class[]     _classes;
  private String[]    _packages;
  private ClassLoader _classLoader;

  public JAXBContextImpl(String contextPath,
			 ClassLoader classLoader,
			 Map<String,?> properties)
  {
    this._classes = new Class[0];
    this._classLoader = classLoader;

    StringTokenizer st = new StringTokenizer(contextPath, ":");
    this._packages = new String[st.countTokens()];

    for(int i=0; i<_packages.length; i++)
      _packages[i] = st.nextToken();

    if (properties != null)
      for(Map.Entry<String,?> e : properties.entrySet())
	setProperty(e.getKey(), e.getValue());
  }

  public JAXBContextImpl(Class[] classes,
			 Map<String,?> properties)
  {
    this._classes = classes;
    this._packages = new String[0];
    this._classLoader = null;

    if (properties != null)
      for(Map.Entry<String,?> e : properties.entrySet())
	setProperty(e.getKey(), e.getValue());
  }

  public Marshaller createMarshaller()
    throws JAXBException
  {
    return new MarshallerImpl(this);
  }

  public Unmarshaller createUnmarshaller()
    throws JAXBException
  {
    return new UnmarshallerImpl(this);
  }

  public Validator createValidator()
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("JAXBContext[");

    for(int i=0; i<_classes.length; i++) {
      Class c = _classes[i];
      sb.append(c.getName() + (i<_classes.length-1 ? ":" : ""));
    }

    for(int i=0; i<_packages.length; i++) {
      String p = _packages[i];
      sb.append(p + (i<_packages.length-1 ? ":" : ""));
    }

    sb.append("]");
    return sb.toString();
  }

  private void setProperty(String key, Object val)
  {
    // XXX
  }

}

