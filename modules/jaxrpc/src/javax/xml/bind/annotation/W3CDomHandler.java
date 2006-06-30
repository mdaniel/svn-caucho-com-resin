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

package javax.xml.bind.annotation;
import javax.xml.transform.*;
import org.w3c.dom.*;
import javax.xml.transform.dom.*;
import javax.xml.parsers.*;
import javax.xml.bind.*;
import org.w3c.dom.Element;

/**
 * DomHandler implementation for W3C DOM (org.w3c.dom package.) Since: JAXB2.0
 * Author: Kohsuke Kawaguchi
 */
public class W3CDomHandler implements DomHandler<org.w3c.dom.Element, DOMResult> {

  /**
   * Default constructor. It is up to a JAXB provider to decide which DOM
   * implementation to use or how that is configured.
   */
  public W3CDomHandler()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructor that allows applications to specify which DOM implementation
   * to be used. Parameters:builder - must not be null. JAXB uses this
   * DocumentBuilder to create a new element.
   */
  public W3CDomHandler(DocumentBuilder builder)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: When a JAXB provider needs to unmarshal
   * a part of a document into an infoset representation, it first calls this
   * method to create a object. A JAXB provider will then send a portion of the
   * XML into the given result. Such a portion always form a subtree of the
   * whole XML document rooted at an element.
   */
  public DOMResult createUnmarshaller(ValidationEventHandler errorHandler)
  {
    throw new UnsupportedOperationException();
  }

  public DocumentBuilder getBuilder()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Once the portion is sent to the . This
   * method is called by a JAXB provider to obtain the unmarshalled element
   * representation. Multiple invocations of this method may return different
   * objects. This method can be invoked only when the whole sub-tree are fed
   * to the Result object.
   */
  public Element getElement(DOMResult r)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: This method is called when a JAXB
   * provider needs to marshal an element to XML. If non-null, the returned
   * Source must contain a whole document rooted at one element, which will
   * then be weaved into a bigger document that the JAXB provider is
   * marshalling.
   */
  public Source marshal(Element element, ValidationEventHandler errorHandler)
  {
    throw new UnsupportedOperationException();
  }

  public void setBuilder(DocumentBuilder builder)
  {
    throw new UnsupportedOperationException();
  }

}

