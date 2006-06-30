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

package javax.xml.bind.util;
import javax.xml.transform.sax.*;
import javax.xml.bind.*;

/**
 * JAXP Source implementation that marshals a JAXB-generated object. This
 * utility class is useful to combine JAXB with other Java/XML technologies.
 * The following example shows how to use JAXB to marshal a document for
 * transformation by XSLT. The fact that JAXBSource derives from SAXSource is
 * an implementation detail. Thus in general applications are strongly
 * discouraged from accessing methods defined on SAXSource. In particular, the
 * setXMLReader and setInputSource methods shall never be called. The XMLReader
 * object obtained by the getXMLReader method shall be used only for parsing
 * the InputSource object returned by the getInputSource method. Similarly the
 * InputSource object obtained by the getInputSource method shall be used only
 * for being parsed by the XMLReader object returned by the getXMLReader.
 * Author: Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class JAXBSource extends SAXSource {

  /**
   * Creates a new for the given content object. Parameters:context -
   * JAXBContext that was used to create contentObject. This context is used to
   * create a new instance of marshaller and must not be null.contentObject -
   * An instance of a JAXB-generated class, which will be used as a Source (by
   * marshalling it into XML). It must not be null. Throws: JAXBException - if
   * an error is encountered while creating the JAXBSource or if either of the
   * parameters are null.
   */
  public JAXBSource(JAXBContext context, Object contentObject) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a new for the given content object. Parameters:marshaller - A
   * marshaller instance that will be used to marshal contentObject into XML.
   * This must be created from a JAXBContext that was used to build
   * contentObject and must not be null.contentObject - An instance of a
   * JAXB-generated class, which will be used as a Source (by marshalling it
   * into XML). It must not be null. Throws: JAXBException - if an error is
   * encountered while creating the JAXBSource or if either of the parameters
   * are null.
   */
  public JAXBSource(Marshaller marshaller, Object contentObject) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

}

