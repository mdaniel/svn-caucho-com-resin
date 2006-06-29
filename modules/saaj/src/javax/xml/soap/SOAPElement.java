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

package javax.xml.soap;
import org.w3c.dom.*;
import javax.xml.namespace.*;
import java.util.*;

/**
 * An object representing an element of a SOAP message that is allowed but not
 * specifically prescribed by a SOAP specification. This interface serves as
 * the base interface for those objects that are specifically prescribed by a
 * SOAP specification. Methods in this interface that are required to return
 * SAAJ specific objects may "silently" replace nodes in the tree as required
 * to successfully return objects of the correct type. See getChildElements()
 * and javax.xml.soap for details.
 */
public interface SOAPElement extends Node, Element {

  /**
   * Adds an attribute with the specified name and value to this SOAPElement
   * object.
   */
  abstract SOAPElement addAttribute(Name name, String value) throws SOAPException;


  /**
   * Adds an attribute with the specified name and value to this SOAPElement
   * object.
   */
  abstract SOAPElement addAttribute(QName qname, String value) throws SOAPException;


  /**
   * Creates a new SOAPElement object initialized with the given Name object
   * and adds the new element to this SOAPElement object. This method may be
   * deprecated in a future release of SAAJ in favor of
   * addChildElement(javax.xml.namespace.QName)
   */
  abstract SOAPElement addChildElement(Name name) throws SOAPException;


  /**
   * Creates a new SOAPElement object initialized with the given QName object
   * and adds the new element to this SOAPElement object. The , and of the new
   * SOAPElement are all taken from the qname argument.
   */
  abstract SOAPElement addChildElement(QName qname) throws SOAPException;


  /**
   * Add a SOAPElement as a child of this SOAPElement instance. The SOAPElement
   * is expected to be created by a SOAPFactory. Callers should not rely on the
   * element instance being added as is into the XML tree. Implementations
   * could end up copying the content of the SOAPElement passed into an
   * instance of a different SOAPElement implementation. For instance if
   * addChildElement() is called on a SOAPHeader, element will be copied into
   * an instance of a SOAPHeaderElement. The fragment rooted in element is
   * either added as a whole or not at all, if there was an error. The fragment
   * rooted in element cannot contain elements named "Envelope", "Header" or
   * "Body" and in the SOAP namespace. Any namespace prefixes present in the
   * fragment should be fully resolved using appropriate namespace declarations
   * within the fragment itself.
   */
  abstract SOAPElement addChildElement(SOAPElement element) throws SOAPException;


  /**
   * Creates a new SOAPElement object initialized with the specified local name
   * and adds the new element to this SOAPElement object. The new SOAPElement
   * inherits any in-scope default namespace.
   */
  abstract SOAPElement addChildElement(String localName) throws SOAPException;


  /**
   * Creates a new SOAPElement object initialized with the specified local name
   * and prefix and adds the new element to this SOAPElement object.
   */
  abstract SOAPElement addChildElement(String localName, String prefix) throws SOAPException;


  /**
   * Creates a new SOAPElement object initialized with the specified local
   * name, prefix, and URI and adds the new element to this SOAPElement object.
   */
  abstract SOAPElement addChildElement(String localName, String prefix, String uri) throws SOAPException;


  /**
   * Adds a namespace declaration with the specified prefix and URI to this
   * SOAPElement object.
   */
  abstract SOAPElement addNamespaceDeclaration(String prefix, String uri) throws SOAPException;


  /**
   * Creates a new Text object initialized with the given String and adds it to
   * this SOAPElement object.
   */
  abstract SOAPElement addTextNode(String text) throws SOAPException;


  /**
   * Creates a QName whose namespace URI is the one associated with the
   * parameter, prefix, in the context of this SOAPElement. The remaining
   * elements of the new QName are taken directly from the parameters,
   * localName and prefix.
   */
  abstract QName createQName(String localName, String prefix) throws SOAPException;


  /**
   * Returns an Iterator over all of the attribute Name objects in this
   * SOAPElement object. The iterator can be used to get the attribute names,
   * which can then be passed to the method getAttributeValue to retrieve the
   * value of each attribute.
   */
  abstract Iterator getAllAttributes();


  /**
   * Returns an Iterator over all of the attributes in this SOAPElement as
   * QName objects. The iterator can be used to get the attribute QName, which
   * can then be passed to the method getAttributeValue to retrieve the value
   * of each attribute.
   */
  abstract Iterator getAllAttributesAsQNames();


  /**
   * Returns the value of the attribute with the specified name.
   */
  abstract String getAttributeValue(Name name);


  /**
   * Returns the value of the attribute with the specified qname.
   */
  abstract String getAttributeValue(QName qname);


  /**
   * Returns an Iterator over all the immediate child s of this element. This
   * includes javax.xml.soap.Text objects as well as SOAPElement objects.
   * Calling this method may cause child Element, SOAPElement and
   * org.w3c.dom.Text nodes to be replaced by SOAPElement, SOAPHeaderElement,
   * SOAPBodyElement or javax.xml.soap.Text nodes as appropriate for the type
   * of this parent node. As a result the calling application must treat any
   * existing references to these child nodes that have been obtained through
   * DOM APIs as invalid and either discard them or refresh them with the
   * values returned by this Iterator. This behavior can be avoided by calling
   * the equivalent DOM APIs. See javax.xml.soap for more details.
   */
  abstract Iterator getChildElements();


  /**
   * Returns an Iterator over all the immediate child s of this element with
   * the specified name. All of these children will be SOAPElement nodes.
   * Calling this method may cause child Element, SOAPElement and
   * org.w3c.dom.Text nodes to be replaced by SOAPElement, SOAPHeaderElement,
   * SOAPBodyElement or javax.xml.soap.Text nodes as appropriate for the type
   * of this parent node. As a result the calling application must treat any
   * existing references to these child nodes that have been obtained through
   * DOM APIs as invalid and either discard them or refresh them with the
   * values returned by this Iterator. This behavior can be avoided by calling
   * the equivalent DOM APIs. See javax.xml.soap for more details.
   */
  abstract Iterator getChildElements(Name name);


  /**
   * Returns an Iterator over all the immediate child s of this element with
   * the specified qname. All of these children will be SOAPElement nodes.
   * Calling this method may cause child Element, SOAPElement and
   * org.w3c.dom.Text nodes to be replaced by SOAPElement, SOAPHeaderElement,
   * SOAPBodyElement or javax.xml.soap.Text nodes as appropriate for the type
   * of this parent node. As a result the calling application must treat any
   * existing references to these child nodes that have been obtained through
   * DOM APIs as invalid and either discard them or refresh them with the
   * values returned by this Iterator. This behavior can be avoided by calling
   * the equivalent DOM APIs. See javax.xml.soap for more details.
   */
  abstract Iterator getChildElements(QName qname);


  /**
   * Returns the name of this SOAPElement object.
   */
  abstract Name getElementName();


  /**
   * Returns the qname of this SOAPElement object.
   */
  abstract QName getElementQName();


  /**
   * Returns the encoding style for this SOAPElement object.
   */
  abstract String getEncodingStyle();


  /**
   * Returns an Iterator over the namespace prefix Strings declared by this
   * element. The prefixes returned by this iterator can be passed to the
   * method getNamespaceURI to retrieve the URI of each namespace.
   */
  abstract Iterator getNamespacePrefixes();


  /**
   * Returns the URI of the namespace that has the given prefix.
   */
  abstract String getNamespaceURI(String prefix);


  /**
   * Returns an Iterator over the namespace prefix Strings visible to this
   * element. The prefixes returned by this iterator can be passed to the
   * method getNamespaceURI to retrieve the URI of each namespace.
   */
  abstract Iterator getVisibleNamespacePrefixes();


  /**
   * Removes the attribute with the specified name.
   */
  abstract boolean removeAttribute(Name name);


  /**
   * Removes the attribute with the specified qname.
   */
  abstract boolean removeAttribute(QName qname);


  /**
   * Detaches all children of this SOAPElement. This method is useful for
   * rolling back the construction of partially completed SOAPHeaders and
   * SOAPBodys in preparation for sending a fault when an error condition is
   * detected. It is also useful for recycling portions of a document within a
   * SOAP message.
   */
  abstract void removeContents();


  /**
   * Removes the namespace declaration corresponding to the given prefix.
   */
  abstract boolean removeNamespaceDeclaration(String prefix);


  /**
   * Changes the name of this Element to newName if possible. SOAP Defined
   * elements such as SOAPEnvelope, SOAPHeader, SOAPBody etc. cannot have their
   * names changed using this method. Any attempt to do so will result in a
   * SOAPException being thrown. Callers should not rely on the element
   * instance being renamed as is. Implementations could end up copying the
   * content of the SOAPElement to a renamed instance.
   */
  abstract SOAPElement setElementQName(QName newName) throws SOAPException;


  /**
   * Sets the encoding style for this SOAPElement object to one specified.
   */
  abstract void setEncodingStyle(String encodingStyle) throws SOAPException;

}

