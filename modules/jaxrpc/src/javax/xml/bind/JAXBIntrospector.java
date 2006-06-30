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
import javax.xml.namespace.*;

/**
 * Provide access to JAXB xml binding data for a JAXB object. Intially, the
 * intent of this class is to just conceptualize how a JAXB application
 * developer can access xml binding information, independent if binding model
 * is java to schema or schema to java. Since accessing the XML element name
 * related to a JAXB element is a highly requested feature, demonstrate access
 * to this binding information. The factory method to get a JAXBIntrospector
 * instance is JAXBContext.createJAXBIntrospector(). Since: JAXB2.0 See
 * Also:JAXBContext.createJAXBIntrospector()
 */
public abstract class JAXBIntrospector {
  public JAXBIntrospector()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Get xml element qname for jaxbElement.
   */
  public abstract QName getElementName(Object jaxbElement);


  /**
   * Get the element value of a JAXB element. Convenience method to abstract
   * whether working with either a javax.xml.bind.JAXBElement instance or an
   * instance of &#64XmlRootElement annotated Java class.
   */
  public static Object getValue(Object jaxbElement)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Return true iff object represents a JAXB element. Parameter object is a
   * JAXB element for following cases: It is an instance of
   * javax.xml.bind.JAXBElement. The class of object is annotated with
   * &#64XmlRootElement.
   */
  public abstract boolean isElement(Object object);

}

