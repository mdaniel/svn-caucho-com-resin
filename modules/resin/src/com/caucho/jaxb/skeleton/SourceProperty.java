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
 * @author Adam Megacz
 */

package com.caucho.jaxb.skeleton;

import javax.xml.bind.JAXBException;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.caucho.util.Base64;

/**
 * a Source Property
 */
public class SourceProperty extends CDataProperty {
  public static final SourceProperty PROPERTY = new SourceProperty();

  private final Transformer _transformer;

  private SourceProperty()
  {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      _transformer = factory.newTransformer();
    }
    catch (TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  protected String write(Object in)
    throws JAXBException
  {
    Source src = (Source) in;
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    try {
      _transformer.transform(src, new StreamResult(out));
    }
    catch (TransformerException e) {
      throw new JAXBException(e);
    }

    return Base64.encodeFromByteArray(out.toByteArray());
  }

  protected Object read(String in)
  {
    byte[] bytes = Base64.decodeToByteArray(in);
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    return new StreamSource(bais);
  }

  public String getSchemaType()
  {
    return "xsd:base64Binary";
  }
}
