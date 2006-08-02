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

package com.caucho.xml.stream;
import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.stream.util.*;
import javax.xml.stream.events.*;
import javax.xml.transform.*;
import javax.xml.namespace.*;

public class XMLEventFactoryImpl extends XMLEventFactory {

  protected XMLEventFactoryImpl()
  {
  }

  public Attribute createAttribute(QName name, String value)
  {
    throw new UnsupportedOperationException();
  }

  public Attribute createAttribute(String localName, String value)
  {
    throw new UnsupportedOperationException();
  }

  public Attribute createAttribute(String prefix, String namespaceURI,
                                   String localName, String value)
  {
    throw new UnsupportedOperationException();
  }

  public Characters createCData(String content)
  {
    throw new UnsupportedOperationException();
  }

  public Characters createCharacters(String content)
  {
    throw new UnsupportedOperationException();
  }

  public Comment createComment(String text)
  {
    throw new UnsupportedOperationException();
  }

  public DTD createDTD(String dtd)
  {
    throw new UnsupportedOperationException();
  }

  public EndDocument createEndDocument()
  {
    throw new UnsupportedOperationException();
  }

  public EndElement createEndElement(QName name, Iterator namespaces)
  {
    throw new UnsupportedOperationException();
  }

  public EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName)
  {
    throw new UnsupportedOperationException();
  }

  public EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName,
                                              Iterator namespaces)
  {
    throw new UnsupportedOperationException();
  }

  public EntityReference
    createEntityReference(String name, EntityDeclaration declaration)
  {
    throw new UnsupportedOperationException();
  }

  public Characters createIgnorableSpace(String content)
  {
    throw new UnsupportedOperationException();
  }

  public Namespace createNamespace(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public Namespace createNamespace(String prefix, String namespaceUri)
  {
    throw new UnsupportedOperationException();
  }

  public ProcessingInstruction
    createProcessingInstruction(String target, String data)
  {
    throw new UnsupportedOperationException();
  }

  public Characters createSpace(String content)
  {
    throw new UnsupportedOperationException();
  }

  public StartDocument createStartDocument()
  {
    throw new UnsupportedOperationException();
  }

  public StartDocument createStartDocument(String encoding)
  {
    throw new UnsupportedOperationException();
  }

  public StartDocument createStartDocument(String encoding,
                                                    String version)
  {
    throw new UnsupportedOperationException();
  }

  public StartDocument createStartDocument(String encoding,
                                                    String version,
                                                    boolean standalone)
  {
    throw new UnsupportedOperationException();
  }

  public StartElement createStartElement(QName name,
                                                  Iterator attributes,
                                                  Iterator namespaces)
  {
    throw new UnsupportedOperationException();
  }

  public StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName)
  {
    throw new UnsupportedOperationException();
  }

  public StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName,
                                                  Iterator attributes,
                                                  Iterator namespaces)
  {
    throw new UnsupportedOperationException();
  }

  public StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName,
                                                  Iterator attributes,
                                                  Iterator namespaces,
                                                  NamespaceContext context)
  {
    throw new UnsupportedOperationException();
  }

  public void setLocation(Location location)
  {
    throw new UnsupportedOperationException();
  }

}

