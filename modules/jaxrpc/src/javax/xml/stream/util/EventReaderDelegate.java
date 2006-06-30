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
 * This is the base class for deriving an XMLEventReader filter. This class is
 * designed to sit between an XMLEventReader and an application's
 * XMLEventReader. By default each method does nothing but call the
 * corresponding method on the parent interface. Version: 1.0 Author: Copyright
 * (c) 2003 by BEA Systems. All Rights Reserved. See Also:XMLEventReader,
 * StreamReaderDelegate
 */
public class EventReaderDelegate implements XMLEventReader {

  /**
   * Construct an empty filter with no parent.
   */
  public EventReaderDelegate()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an filter with the specified parent. Parameters:reader - the
   * parent
   */
  public EventReaderDelegate(XMLEventReader reader)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Frees any resources associated with
   * this Reader. This method does not close the underlying input source.
   */
  public void close() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Reads the content of a text-only
   * element. Precondition: the current event is START_ELEMENT. Postcondition:
   * The current event is the corresponding END_ELEMENT.
   */
  public String getElementText() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Get the parent of this instance.
   */
  public XMLEventReader getParent()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the value of a feature/property
   * from the underlying implementation
   */
  public Object getProperty(String name) throws IllegalArgumentException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Check if there are more events. Returns
   * true if there are more events and false otherwise.
   */
  public boolean hasNext()
  {
    throw new UnsupportedOperationException();
  }

  public Object next()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Get the next XMLEvent
   */
  public XMLEvent nextEvent() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Skips any insignificant space events
   * until a START_ELEMENT or END_ELEMENT is reached. If anything other than
   * space characters are encountered, an exception is thrown. This method
   * should be used when processing element-only content because the parser is
   * not able to recognize ignorable whitespace if the DTD is missing or not
   * interpreted.
   */
  public XMLEvent nextTag() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Check the next XMLEvent without reading
   * it from the stream. Returns null if the stream is at EOF or has no more
   * XMLEvents. A call to peek() will be equal to the next return of next().
   */
  public XMLEvent peek() throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the parent of this instance.
   */
  public void setParent(XMLEventReader reader)
  {
    throw new UnsupportedOperationException();
  }

}

