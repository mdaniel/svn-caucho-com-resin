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

package javax.xml.stream.util;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

/**
 * This interface defines a class that allows a user to register a way to
 * allocate events given an XMLStreamReader. An implementation is not required
 * to use the XMLEventFactory implementation but this is recommended. The
 * XMLEventAllocator can be set on an XMLInputFactory using the property
 * "javax.xml.stream.allocator" Version: 1.0 Author: Copyright (c) 2003 by BEA
 * Systems. All Rights Reserved. See Also:XMLInputFactory, XMLEventFactory
 */
public interface XMLEventAllocator {

  /**
   * This method allocates an event given the current state of the
   * XMLStreamReader. If this XMLEventAllocator does not have a one-to-one
   * mapping between reader states and events this method will return null.
   * This method must not modify the state of the XMLStreamReader.
   */
  abstract XMLEvent allocate(XMLStreamReader reader) throws XMLStreamException;


  /**
   * This method allocates an event or set of events given the current state of
   * the XMLStreamReader and adds the event or set of events to the consumer
   * that was passed in. This method can be used to expand or contract reader
   * states into event states. This method may modify the state of the
   * XMLStreamReader.
   */
  abstract void allocate(XMLStreamReader reader, XMLEventConsumer consumer) throws XMLStreamException;


  /**
   * This method creates an instance of the XMLEventAllocator. This allows the
   * XMLInputFactory to allocate a new instance per reader.
   */
  abstract XMLEventAllocator newInstance();

}

