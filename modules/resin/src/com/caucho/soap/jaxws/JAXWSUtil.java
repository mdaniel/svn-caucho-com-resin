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

package com.caucho.soap.jaxws;

import java.io.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;

import com.caucho.util.L10N;

import com.caucho.xml.stream.StaxUtil;
import com.caucho.xml.stream.XMLStreamReaderImpl;
import com.caucho.xml.stream.XMLStreamWriterImpl;

public class JAXWSUtil {
  private final static L10N L = new L10N(JAXWSUtil.class);

  public static void writeStartSOAPEnvelope(Writer out, String namespace)
    throws IOException
  {
    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    out.write("<soapenv:Envelope xmlns:soapenv=\"" + namespace + "\">");
    out.write("<soapenv:Body>");

    out.flush();
  }

  public static void writeEndSOAPEnvelope(Writer out)
    throws IOException
  {
    out.write("</soapenv:Body>");
    out.write("</soapenv:Envelope>");

    out.flush();
  }

  public static void extractSOAPBody(InputStream in, OutputStream out)
    throws WebServiceException
  {
    boolean foundBody = false;

    try {
      XMLStreamReaderImpl reader = new XMLStreamReaderImpl(in);

      // skip the Envelope
      reader.nextTag();

      if (reader.getEventType() != reader.START_ELEMENT ||
          ! "Envelope".equals(reader.getLocalName())) {
        throw new WebServiceException(L.l("Invalid response from server: No Envelope found"));
      }

      // find the body
      while (reader.hasNext()) {
        reader.next();

        if (reader.getEventType() == reader.START_ELEMENT &&
            "Body".equals(reader.getLocalName())) {

          // Copy the body contents to a StreamDataHandler
          reader.nextTag();

          XMLStreamWriterImpl xmlWriter = new XMLStreamWriterImpl(out, false);

          StaxUtil.copyReaderToWriter(reader, xmlWriter);

          xmlWriter.flush();

          foundBody = true;

          break;
        }
      }
    }
    catch (XMLStreamException e) {
      throw new WebServiceException(e);
    }

    if (! foundBody)
      throw new WebServiceException(L.l("Invalid response from server"));
  }
}
