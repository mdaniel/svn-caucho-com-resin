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
* @author Scott Ferguson
*/

package javax.xml.stream;
import javax.xml.transform.*;
import java.io.*;

public abstract class XMLOutputFactory {

  public static final String IS_REPAIRING_NAMESPACES =
    "javax.xml.stream.isRepairingNamespaces";

  protected XMLOutputFactory()
  {
  }

  public abstract XMLEventWriter createXMLEventWriter(OutputStream stream)
    throws XMLStreamException;

  public abstract XMLEventWriter createXMLEventWriter(OutputStream stream,
						      String encoding)
    throws XMLStreamException;

  public abstract XMLEventWriter createXMLEventWriter(Result result)
    throws XMLStreamException;

  public abstract XMLEventWriter createXMLEventWriter(Writer stream)
    throws XMLStreamException;

  public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream)
    throws XMLStreamException;

  public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream,
							String encoding)
    throws XMLStreamException;

  public abstract XMLStreamWriter createXMLStreamWriter(Result result)
    throws XMLStreamException;

  public abstract XMLStreamWriter createXMLStreamWriter(Writer stream)
    throws XMLStreamException;

  public abstract Object getProperty(String name)
    throws IllegalArgumentException;

  public abstract boolean isPropertySupported(String name);

  public static XMLOutputFactory newInstance() throws FactoryConfigurationError
  {
    return newInstance("javax.xml.stream.XMLOutputFactory",
		       Thread.currentThread().getContextClassLoader());
  }

  public static XMLOutputFactory newInstance(String factoryId,
					     ClassLoader classLoader)
    throws FactoryConfigurationError
  {
    return (XMLOutputFactory)FactoryLoader
      .getFactoryLoader(factoryId).newInstance(classLoader);
  }

  public abstract void setProperty(String name, Object value)
    throws IllegalArgumentException;

}

