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

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.Base64;

/**
 * DataHandler property
 */
// XXX Needs to read an attribute on the xml to discover the appropriate
// mime type -> don't subclass CDataProperty
public class DataHandlerProperty extends CDataProperty {
  public static final DataHandlerProperty PROPERTY = new DataHandlerProperty();

  protected String write(Object in)
  {
    try {
      DataHandler handler = (DataHandler) in;

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      handler.writeTo(baos);

      return Base64.encodeFromByteArray(baos.toByteArray());
    }
    catch (IOException e) {
      // XXX
      return null;
    }
  }

  protected Object read(String in)
    throws JAXBException
  {
    byte[] buffer = Base64.decodeToByteArray(in);
    // XXX mime type
    ByteArrayDataSource dataSource 
      = new ByteArrayDataSource(buffer, "text/XXX");

    return new DataHandler(dataSource);
  }

  public String getSchemaType()
  {
    return "xsd:base64Binary";
  }
}
