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
 * This interface defines an event consumer interface. The contract of the of a
 * consumer is to accept the event. This interface can be used to mark an
 * object as able to receive events. Add may be called several times in
 * immediate succession so a consumer must be able to cache events it hasn't
 * processed yet. Version: 1.0 Author: Copyright (c) 2003 by BEA Systems. All
 * Rights Reserved.
 */
public interface XMLEventConsumer {

  /**
   * This method adds an event to the consumer. Calling this method invalidates
   * the event parameter. The client application should discard all references
   * to this event upon calling add. The behavior of an application that
   * continues to use such references is undefined.
   */
  abstract void add(XMLEvent event) throws XMLStreamException;

}

