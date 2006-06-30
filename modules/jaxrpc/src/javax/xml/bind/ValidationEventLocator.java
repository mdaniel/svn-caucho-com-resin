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

package javax.xml.bind;
import org.w3c.dom.*;
import java.net.*;

/**
 * Encapsulate the location of a ValidationEvent. The ValidationEventLocator
 * indicates where the ValidationEvent occurred. Different fields will be set
 * depending on the type of validation that was being performed when the error
 * or warning was detected. For example, on-demand validation would produce
 * locators that contained references to objects in the Java content tree while
 * unmarshal-time validation would produce locators containing information
 * appropriate to the source of the XML data (file, url, Node, etc). Since:
 * JAXB1.0 Version: $Revision: 1.1 $ Author: Ryan Shoemaker, Sun Microsystems,
 * Inc.Kohsuke Kawaguchi, Sun Microsystems, Inc.Joe Fialli, Sun Microsystems,
 * Inc. See Also:Validator, ValidationEvent
 */
public interface ValidationEventLocator {

  /**
   * Return the column number if available
   */
  abstract int getColumnNumber();


  /**
   * Return the line number if available
   */
  abstract int getLineNumber();


  /**
   * Return a reference to the DOM Node if available
   */
  abstract Node getNode();


  /**
   * Return a reference to the object in the Java content tree if available
   */
  abstract Object getObject();


  /**
   * Return the byte offset if available
   */
  abstract int getOffset();


  /**
   * Return the name of the XML source as a URL if available
   */
  abstract URL getURL();

}

