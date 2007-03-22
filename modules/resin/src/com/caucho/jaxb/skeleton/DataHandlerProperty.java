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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.BinderImpl;

import com.caucho.util.L10N;
import com.caucho.util.Base64;

/**
 * DataHandler property.
 *
 * Note that DataHandler fields/properties are not affected by XmlMimeType
 * annotations.
 */
public class DataHandlerProperty extends CDataProperty {
  public static final DataHandlerProperty PROPERTY = new DataHandlerProperty();
  private static final String DEFAULT_DATA_HANDLER_MIME_TYPE 
    = "application/octet-stream";

  protected Object read(String in) 
    throws IOException, JAXBException
  {
    byte[] buffer = Base64.decodeToByteArray(in);
    ByteArrayDataSource bads = 
      new ByteArrayDataSource(buffer, DEFAULT_DATA_HANDLER_MIME_TYPE);
    return new DataHandler(bads);
  }

  protected String write(Object value)
    throws IOException, JAXBException
  {
    if (value != null) {
      DataHandler handler = (DataHandler) value;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      handler.writeTo(baos);

      return Base64.encodeFromByteArray(baos.toByteArray());
    }

    return "";
  }

  public String getSchemaType()
  {
    return "xsd:base64Binary";
  }
}
