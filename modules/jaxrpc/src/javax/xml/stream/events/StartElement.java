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

package javax.xml.stream.events;
import javax.xml.namespace.*;
import java.util.*;

/**
 * The StartElement interface provides access to information about start
 * elements. A StartElement is reported for each Start Tag in the document.
 * Version: 1.0 Author: Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 */
public interface StartElement extends XMLEvent {

  /**
   * Returns the attribute referred to by this name
   */
  abstract Attribute getAttributeByName(QName name);


  /**
   * Returns an Iterator of non-namespace declared attributes declared on this
   * START_ELEMENT, returns an empty iterator if there are no attributes. The
   * iterator must contain only implementations of the
   * javax.xml.stream.Attribute interface. Attributes are fundamentally
   * unordered and may not be reported in any order.
   */
  abstract Iterator getAttributes();


  /**
   * Get the name of this event
   */
  abstract QName getName();


  /**
   * Gets a read-only namespace context. If no context is available this method
   * will return an empty namespace context. The NamespaceContext contains
   * information about all namespaces in scope for this StartElement.
   */
  abstract NamespaceContext getNamespaceContext();


  /**
   * Returns an Iterator of namespaces declared on this element. This Iterator
   * does not contain previously declared namespaces unless they appear on the
   * current START_ELEMENT. Therefore this list may contain redeclared
   * namespaces and duplicate namespace declarations. Use the
   * getNamespaceContext() method to get the current context of namespace
   * declarations. The iterator must contain only implementations of the
   * javax.xml.stream.Namespace interface. A Namespace isA Attribute. One can
   * iterate over a list of namespaces as a list of attributes. However this
   * method returns only the list of namespaces declared on this START_ELEMENT
   * and does not include the attributes declared on this START_ELEMENT.
   * Returns an empty iterator if there are no namespaces.
   */
  abstract Iterator getNamespaces();


  /**
   * Gets the value that the prefix is bound to in the context of this element.
   * Returns null if the prefix is not bound in this context
   */
  abstract String getNamespaceURI(String prefix);

}

