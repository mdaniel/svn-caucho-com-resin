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
import org.xml.sax.*;

/**
 * Unmarshaller implemented as SAX ContentHandler. Applications can use this
 * interface to use their JAXB provider as a component in an XML pipeline. For
 * example: JAXBContext context = JAXBContext.newInstance( "org.acme.foo" );
 * Unmarshaller unmarshaller = context.createUnmarshaller();
 * UnmarshallerHandler unmarshallerHandler =
 * unmarshaller.getUnmarshallerHandler(); SAXParserFactory spf =
 * SAXParserFactory.newInstance(); spf.setNamespaceAware( true ); XMLReader
 * xmlReader = spf.newSAXParser().getXMLReader(); xmlReader.setContentHandler(
 * unmarshallerHandler ); xmlReader.parse(new InputSource( new FileInputStream(
 * XML_FILE ) ) ); MyObject myObject=
 * (MyObject)unmarshallerHandler.getResult(); This interface is reusable: even
 * if the user fails to unmarshal an object, s/he can still start a new round
 * of unmarshalling. Since: JAXB1.0 Version: $Revision: 1.2 $ $Date: 2006/03/08
 * 16:55:17 $ Author: Kohsuke KAWAGUCHI, Sun Microsystems, Inc. See
 * Also:Unmarshaller.getUnmarshallerHandler()
 */
public interface UnmarshallerHandler extends ContentHandler {

  /**
   * Obtains the unmarshalled result. This method can be called only after this
   * handler receives the endDocument SAX event.
   */
  abstract Object getResult() throws JAXBException, IllegalStateException;

}

