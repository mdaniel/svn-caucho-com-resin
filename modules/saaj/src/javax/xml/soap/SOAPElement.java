/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.xml.soap;

import java.util.Iterator;

/**
 * Implements a SOAP extension of the DOM Element.
 */
public interface SOAPElement extends Node, org.w3c.dom.Element {
  /**
   * Creates a new child element.
   */
  public SOAPElement addChildElement(Name name)
    throws SOAPException;
  
  /**
   * Creates a new child element.
   */
  public SOAPElement addChildElement(String localName)
    throws SOAPException;
  
  /**
   * Creates a new child element.
   */
  public SOAPElement addChildElement(String localName, String prefix)
    throws SOAPException;
  
  /**
   * Creates a new child element.
   */
  public SOAPElement addChildElement(String localName,
				     String prefix,
				     String uri)
    throws SOAPException;
  
  /**
   * Creates a new child element.
   */
  public SOAPElement addChildElement(SOAPElement element)
    throws SOAPException;
  
  /**
   * Removes the contents of the SOAPElement.
   */
  public void removeContents()
    throws SOAPException;
  
  /**
   * Adds a text node
   */
  public SOAPElement addTextNode(String text)
    throws SOAPException;
  
  /**
   * Adds an attribute
   */
  public SOAPElement addAttribute(Name name, String value)
    throws SOAPException;
  
  /**
   * Adds a namespace declaration
   */
  public SOAPElement addNamespaceDeclaration(String prefix, String uri)
    throws SOAPException;
  
  /**
   * Returns the value.
   */
  public String getAttributeValue(Name name);
  
  /**
   * Returns the attribute names.
   */
  public Iterator getAllAttributes();
  
  /**
   * Returns the namespace uri for the prefix
   */
  public String getNamespaceURI(String prefix);
  
  /**
   * Returns the namespace prefixes names.
   */
  public Iterator getNamespacePrefixes();
  
  /**
   * Returns the visible namespace prefixes
   */
  public Iterator getVisibleNamespacePrefixes();
  
  /**
   * Returns the element name.
   */
  public Name getElementName();
  
  /**
   * Removes the named attribute
   */
  public boolean removeAttribute(Name name);
  
  /**
   * Removes the namespace declaration
   */
  public boolean removeNamespaceDeclaration(String prefix);
  
  /**
   * Returns the child elements.
   */
  public Iterator getChildElements();
  
  /**
   * Returns the child elements with the given name.
   */
  public Iterator getChildElements(Name name);
  
  /**
   * Sets the encoding style.
   */
  public void setEncodingStyle(String encodingStyle)
    throws SOAPException;
  
  /**
   * Gets the encoding style.
   */
  public String getEncodingStyle();
}
