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
* @author Emil Ong
*/

package com.caucho.xml.stream.events;

import java.io.*;
import java.util.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

public abstract class XMLEventImpl implements XMLEvent {

  public Characters asCharacters()
  {
    return (Characters) this;
  }

  public EndElement asEndElement()
  {
    return (EndElement) this;
  }

  public StartElement asStartElement()
  {
    return (StartElement) this;
  }

  public abstract int getEventType();

  public Location getLocation()
  {
    throw new UnsupportedOperationException();
  }

  public QName getSchemaType()
  {
    return null;
  }

  public boolean isAttribute()
  {
    return false;
  }

  public boolean isCharacters()
  {
    return false;
  }

  public boolean isEndDocument()
  {
    return false;
  }

  public boolean isEndElement()
  {
    return false;
  }

  public boolean isEntityReference()
  {
    return false;
  }

  public boolean isNamespace()
  {
    return false;
  }

  public boolean isProcessingInstruction()
  {
    return false;
  }

  public boolean isStartDocument()
  {
    return false;
  }

  public boolean isStartElement()
  {
    return false;
  }

  public abstract void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException;
}

