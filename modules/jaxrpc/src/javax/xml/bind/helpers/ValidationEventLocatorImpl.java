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

package javax.xml.bind.helpers;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.bind.*;
import java.net.*;

/**
 * Default implementation of the ValidationEventLocator interface. JAXB
 * providers are allowed to use whatever class that implements the
 * ValidationEventLocator interface. This class is just provided for a
 * convenience. Since: JAXB1.0 Version: $Revision: 1.2 $ Author: Kohsuke
 * Kawaguchi, Sun Microsystems, Inc. See Also:Validator,
 * ValidationEventHandler, ValidationEvent, ValidationEventLocator
 */
public class ValidationEventLocatorImpl implements ValidationEventLocator {

  /**
   * Creates an object with all fields unavailable.
   */
  public ValidationEventLocatorImpl()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs an object from an org.xml.sax.Locator. The object's
   * ColumnNumber, LineNumber, and URL become available from the values
   * returned by the locator's getColumnNumber(), getLineNumber(), and
   * getSystemId() methods respectively. Node, Object, and Offset are not
   * available. Parameters:loc - the SAX Locator object that will be used to
   * populate this event locator. Throws: IllegalArgumentException - if the
   * Locator is null
   */
  public ValidationEventLocatorImpl(Locator loc)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs an object that points to a DOM Node. The object's Node becomes
   * available. ColumnNumber, LineNumber, Object, Offset, and URL are not
   * available. Parameters:_node - the DOM Node object that will be used to
   * populate this event locator. Throws: IllegalArgumentException - if the
   * Node is null
   */
  public ValidationEventLocatorImpl(Node _node)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs an object that points to a JAXB content object. The object's
   * Object becomes available. ColumnNumber, LineNumber, Node, Offset, and URL
   * are not available. Parameters:_object - the Object that will be used to
   * populate this event locator. Throws: IllegalArgumentException - if the
   * Object is null
   */
  public ValidationEventLocatorImpl(Object _object)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs an object from the location information of a SAXParseException.
   * The object's ColumnNumber, LineNumber, and URL become available from the
   * values returned by the locator's getColumnNumber(), getLineNumber(), and
   * getSystemId() methods respectively. Node, Object, and Offset are not
   * available. Parameters:e - the SAXParseException object that will be used
   * to populate this event locator. Throws: IllegalArgumentException - if the
   * SAXParseException is null
   */
  public ValidationEventLocatorImpl(SAXParseException e)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return the column number if available
   */
  public int getColumnNumber()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return the line number if available
   */
  public int getLineNumber()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return a reference to the DOM Node if
   * available
   */
  public Node getNode()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return a reference to the object in the
   * Java content tree if available
   */
  public Object getObject()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return the byte offset if available
   */
  public int getOffset()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Return the name of the XML source as a
   * URL if available
   */
  public URL getURL()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the columnNumber field on this event locator.
   */
  public void setColumnNumber(int _columnNumber)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the lineNumber field on this event locator.
   */
  public void setLineNumber(int _lineNumber)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the Node field on this event locator. Null values are allowed.
   */
  public void setNode(Node _node)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the Object field on this event locator. Null values are allowed.
   */
  public void setObject(Object _object)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the offset field on this event locator.
   */
  public void setOffset(int _offset)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the URL field on this event locator. Null values are allowed.
   */
  public void setURL(URL _url)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns a string representation of this object in a format helpful to
   * debugging.
   */
  public String toString()
  {
    throw new UnsupportedOperationException();
  }

}

