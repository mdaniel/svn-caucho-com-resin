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
 * JAXP Result implementation that unmarshals a JAXB object. This utility class
 * is useful to combine JAXB with other Java/XML technologies. The following
 * example shows how to use JAXB to unmarshal a document resulting from an XSLT
 * transformation. The fact that JAXBResult derives from SAXResult is an
 * implementation detail. Thus in general applications are strongly discouraged
 * from accessing methods defined on SAXResult. In particular it shall never
 * attempt to call the setHandler, setLexicalHandler, and setSystemId methods.
 * Author: Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class JAXBResult extends SAXResult {

  /**
   * Creates a new instance that uses the specified JAXBContext to unmarshal.
   * Parameters:context - The JAXBContext that will be used to create the
   * necessary Unmarshaller. This parameter must not be null. Throws:
   * JAXBException - if an error is encountered while creating the JAXBResult
   * or if the context parameter is null.
   */
  public JAXBResult(JAXBContext context) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a new instance that uses the specified Unmarshaller to unmarshal
   * an object. This JAXBResult object will use the specified Unmarshaller
   * instance. It is the caller's responsibility not to use the same
   * Unmarshaller for other purposes while it is being used by this object. The
   * primary purpose of this method is to allow the client to configure
   * Unmarshaller. Unless you know what you are doing, it's easier and safer to
   * pass a JAXBContext. Parameters:_unmarshaller - the unmarshaller. This
   * parameter must not be null. Throws: JAXBException - if an error is
   * encountered while creating the JAXBResult or the Unmarshaller parameter is
   * null.
   */
  public JAXBResult(Unmarshaller _unmarshaller) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the unmarshalled object created by the transformation.
   */
  public Object getResult() throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

}

