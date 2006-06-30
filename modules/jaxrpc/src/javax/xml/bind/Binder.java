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
import javax.xml.validation.*;

/**
 * Enable synchronization between XML infoset nodes and JAXB objects
 * representing same XML document. An instance of this class maintains the
 * association between XML nodes of an infoset preserving view and a JAXB
 * representation of an XML document. Navigation between the two views is
 * provided by the methods getXMLNode(Object) and getJAXBNode(Object).
 * Modifications can be made to either the infoset preserving view or the JAXB
 * representation of the document while the other view remains unmodified. The
 * binder is able to synchronize the changes made in the modified view back
 * into the other view using the appropriate Binder update methods,
 * updateXML(Object, Object) or updateJAXB(Object). A typical usage scenario is
 * the following: load XML document into an XML infoset representation
 * unmarshal(Object) XML infoset view to JAXB view. (Note to conserve
 * resources, it is possible to only unmarshal a subtree of the XML infoset
 * view to the JAXB view.) application access/updates JAXB view of XML
 * document. updateXML(Object) synchronizes modifications to JAXB view back
 * into the XML infoset view. Update operation preserves as much of original
 * XML infoset as possible (i.e. comments, PI, ...) A Binder instance is
 * created using the factory method JAXBContext.createBinder() or
 * JAXBContext.createBinder(Class). The template parameter, XmlNode, is the
 * root interface/class for the XML infoset preserving representation. A Binder
 * implementation is required to minimally support an XmlNode value of
 * org.w3c.dom.Node.class. A Binder implementation can support alternative XML
 * infoset preserving representations. Since: JAXB 2.0 Author: Kohsuke
 * Kawaguchi (kohsuke.kawaguchi@sun.com) Joseph Fialli
 */
public abstract class Binder<XmlNode> {
  public Binder()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Return the current event handler or the default event handler if one
   * hasn't been set.
   */
  public abstract ValidationEventHandler getEventHandler() throws JAXBException;


  /**
   * Gets the JAXB object associated with the given XML element. Once a JAXB
   * object tree is associated with an XML fragment, this method enables
   * navigation between the two trees. An association between an XML element
   * and a JAXB object is established by the unmarshal, marshal and update
   * methods. Note that this association is partial; not all XML elements have
   * associated JAXB objects, and not all JAXB objects have associated XML
   * elements.
   */
  public abstract Object getJAXBNode(XmlNode xmlNode);


  /**
   * Get the particular property in the underlying implementation of Binder.
   * This method can only be used to get one of the standard JAXB defined
   * unmarshal/marshal properties or a provider specific property for binder,
   * unmarshal or marshal. Attempting to get an undefined property will result
   * in a PropertyException being thrown. See and .
   */
  public abstract Object getProperty(String name) throws PropertyException;


  /**
   * Gets the last object (including null) set by the method.
   */
  public abstract Schema getSchema();


  /**
   * Gets the XML element associated with the given JAXB object. Once a JAXB
   * object tree is associated with an XML fragment, this method enables
   * navigation between the two trees. An association between an XML element
   * and a JAXB object is established by the bind methods and the update
   * methods. Note that this association is partial; not all XML elements have
   * associated JAXB objects, and not all JAXB objects have associated XML
   * elements.
   */
  public abstract XmlNode getXMLNode(Object jaxbObject);


  /**
   * Marshal a JAXB object tree to a new XML document. This method is similar
   * to Marshaller.marshal(Object, Node) with the addition of maintaining the
   * association between JAXB objects and the produced XML nodes, enabling
   * future update operations such as updateXML(Object, Object) or
   * updateJAXB(Object). When getSchema() is non-null, the marshalled xml
   * content is validated during this operation.
   */
  public abstract void marshal(Object jaxbObject, XmlNode xmlNode) throws JAXBException;


  /**
   * Allow an application to register a ValidationEventHandler. The
   * ValidationEventHandler will be called by the JAXB Provider if any
   * validation errors are encountered during calls to any of the Binder
   * unmarshal, marshal and update methods. Calling this method with a null
   * parameter will cause the Binder to revert back to the default default
   * event handler.
   */
  public abstract void setEventHandler(ValidationEventHandler handler) throws JAXBException;


  /**
   * Set the particular property in the underlying implementation of Binder.
   * This method can only be used to set one of the standard JAXB defined
   * unmarshal/marshal properties or a provider specific property for binder,
   * unmarshal or marshal. Attempting to set an undefined property will result
   * in a PropertyException being thrown. See and .
   */
  public abstract void setProperty(String name, Object value) throws PropertyException;


  /**
   * Specifies whether marshal, unmarshal and update methods performs
   * validation on their XML content.
   */
  public abstract void setSchema(Schema schema);


  /**
   * Unmarshal XML infoset view to a JAXB object tree. This method is similar
   * to Unmarshaller.unmarshal(Node) with the addition of maintaining the
   * association between XML nodes and the produced JAXB objects, enabling
   * future update operations, updateXML(Object, Object) or updateJAXB(Object).
   * When getSchema() is non-null, xmlNode and its descendants is validated
   * during this operation. This method throws UnmarshalException when the
   * Binder's JAXBContext does not have a mapping for the XML element name or
   * the type, specifiable via @xsi:type, of xmlNode to a JAXB mapped class.
   * The method unmarshal(Object, Class) enables an application to specify the
   * JAXB mapped class that the xmlNode should be mapped to.
   */
  public abstract Object unmarshal(XmlNode xmlNode) throws JAXBException;


  /**
   * Unmarshal XML root element by provided declaredType to a JAXB object tree.
   * Implements Unmarshal by Declared Type This method is similar to
   * Unmarshaller.unmarshal(Node, Class) with the addition of maintaining the
   * association between XML nodes and the produced JAXB objects, enabling
   * future update operations, updateXML(Object, Object) or updateJAXB(Object).
   * When getSchema() is non-null, xmlNode and its descendants is validated
   * during this operation.
   */
  abstract <T> JAXBElement<T> unmarshal(XmlNode node, Class<T> declaredType)
      throws JAXBException;


  /**
   * Takes an XML node and updates its associated JAXB object and its
   * descendants. This operation can be thought of as an "in-place"
   * unmarshalling. The difference is that instead of creating a whole new JAXB
   * tree, this operation updates an existing tree, reusing as much JAXB
   * objects as possible. As a side-effect, this operation updates the
   * association between XML nodes and JAXB objects.
   */
  public abstract Object updateJAXB(XmlNode xmlNode) throws JAXBException;


  /**
   * Takes an JAXB object and updates its associated XML node and its
   * descendants. This is a convenience method of: updateXML( jaxbObject,
   * getXMLNode(jaxbObject));
   */
  public abstract XmlNode updateXML(Object jaxbObject) throws JAXBException;


  /**
   * Changes in JAXB object tree are updated in its associated XML parse tree.
   * This operation can be thought of as an "in-place" marshalling. The
   * difference is that instead of creating a whole new XML tree, this
   * operation updates an existing tree while trying to preserve the XML as
   * much as possible. For example, unknown elements/attributes in XML that
   * were not bound to JAXB will be left untouched (whereas a marshalling
   * operation would create a new tree that doesn't contain any of those.) As a
   * side-effect, this operation updates the association between XML nodes and
   * JAXB objects.
   */
  public abstract XmlNode updateXML(Object jaxbObject, XmlNode xmlNode)
      throws JAXBException;

}

