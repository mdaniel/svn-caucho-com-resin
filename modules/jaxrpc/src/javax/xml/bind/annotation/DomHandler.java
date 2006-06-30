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
import javax.xml.bind.*;

/**
 * Converts an element (and its descendants) from/to DOM (or similar)
 * representation. Implementations of this interface will be used in
 * conjunction with XmlAnyElement annotation to map an element of XML into a
 * representation of infoset such as W3C DOM. Implementations hide how a
 * portion of XML is converted into/from such DOM-like representation, allowing
 * JAXB providers to work with arbitrary such library. This interface is
 * intended to be implemented by library writers and consumed by JAXB
 * providers. None of those methods are intended to be called from
 * applications. Since: JAXB2.0 Author: Kohsuke Kawaguchi
 */
public interface DomHandler<ElementT,ResultT extends Result> {

  /**
   * When a JAXB provider needs to unmarshal a part of a document into an
   * infoset representation, it first calls this method to create a object. A
   * JAXB provider will then send a portion of the XML into the given result.
   * Such a portion always form a subtree of the whole XML document rooted at
   * an element.
   */
  abstract ResultT createUnmarshaller(ValidationEventHandler errorHandler);


  /**
   * Once the portion is sent to the . This method is called by a JAXB provider
   * to obtain the unmarshalled element representation. Multiple invocations of
   * this method may return different objects. This method can be invoked only
   * when the whole sub-tree are fed to the Result object.
   */
  abstract ElementT getElement(ResultT rt);


  /**
   * This method is called when a JAXB provider needs to marshal an element to
   * XML. If non-null, the returned Source must contain a whole document rooted
   * at one element, which will then be weaved into a bigger document that the
   * JAXB provider is marshalling.
   */
  abstract Source marshal(ElementT n, ValidationEventHandler errorHandler);

}

