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

/**
 * SOAPFactory is a factory for creating various objects that exist in the SOAP
 * XML tree. SOAPFactory can be used to create XML fragments that will
 * eventually end up in the SOAP part. These fragments can be inserted as
 * children of the SOAPHeaderElement or SOAPBodyElement or SOAPEnvelope or
 * other SOAPElement objects. SOAPFactory also has methods to create
 * javax.xml.soap.Detail objects as well as java.xml.soap.Name objects.
 */
public abstract class SOAPFactory {
  public SOAPFactory()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a new Detail object which serves as a container for DetailEntry
   * objects. This factory method creates Detail objects for use in situations
   * where it is not practical to use the SOAPFault abstraction.
   */
  public abstract Detail createDetail() throws SOAPException;


  /**
   * Creates a SOAPElement object from an existing DOM Element. If the DOM
   * Element that is passed in as an argument is already a SOAPElement then
   * this method must return it unmodified without any further work. Otherwise,
   * a new SOAPElement is created and a deep copy is made of the domElement
   * argument. The concrete type of the return value will depend on the name of
   * the domElement argument. If any part of the tree rooted in domElement
   * violates SOAP rules, a SOAPException will be thrown.
   */
  public SOAPElement createElement(Element domElement) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a SOAPElement object initialized with the given Name object. The
   * concrete type of the return value will depend on the name given to the new
   * SOAPElement. For instance, a new SOAPElement with the name
   * "{http://www.w3.org/2003/05/soap-envelope}Envelope" would cause a
   * SOAPEnvelope that supports SOAP 1.2 behavior to be created.
   */
  public abstract SOAPElement createElement(Name name) throws SOAPException;


  /**
   * Creates a SOAPElement object initialized with the given QName object. The
   * concrete type of the return value will depend on the name given to the new
   * SOAPElement. For instance, a new SOAPElement with the name
   * "{http://www.w3.org/2003/05/soap-envelope}Envelope" would cause a
   * SOAPEnvelope that supports SOAP 1.2 behavior to be created.
   */
  public SOAPElement createElement(QName qname) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a SOAPElement object initialized with the given local name.
   */
  public abstract SOAPElement createElement(String localName) throws SOAPException;


  /**
   * Creates a new SOAPElement object with the given local name, prefix and
   * uri. The concrete type of the return value will depend on the name given
   * to the new SOAPElement. For instance, a new SOAPElement with the name
   * "{http://www.w3.org/2003/05/soap-envelope}Envelope" would cause a
   * SOAPEnvelope that supports SOAP 1.2 behavior to be created.
   */
  public abstract SOAPElement createElement(String localName, String prefix, String uri) throws SOAPException;


  /**
   * Creates a new default SOAPFault object
   */
  public abstract SOAPFault createFault() throws SOAPException;


  /**
   * Creates a new SOAPFault object initialized with the given reasonText and
   * faultCode
   */
  public abstract SOAPFault createFault(String reasonText, QName faultCode) throws SOAPException;


  /**
   * Creates a new Name object initialized with the given local name. This
   * factory method creates Name objects for use in situations where it is not
   * practical to use the SOAPEnvelope abstraction.
   */
  public abstract Name createName(String localName) throws SOAPException;


  /**
   * Creates a new Name object initialized with the given local name, namespace
   * prefix, and namespace URI. This factory method creates Name objects for
   * use in situations where it is not practical to use the SOAPEnvelope
   * abstraction.
   */
  public abstract Name createName(String localName, String prefix, String uri) throws SOAPException;


  /**
   * Creates a new SOAPFactory object that is an instance of the default
   * implementation (SOAP 1.1), This method uses the following ordered lookup
   * procedure to determine the SOAPFactory implementation class to load: Use
   * the javax.xml.soap.SOAPFactory system property. Use the properties file
   * "lib/jaxm.properties" in the JRE directory. This configuration file is in
   * standard java.util.Properties format and contains the fully qualified name
   * of the implementation class with the key being the system property defined
   * above. Use the Services API (as detailed in the JAR specification), if
   * available, to determine the classname. The Services API will look for a
   * classname in the file META-INF/services/javax.xml.soap.SOAPFactory in jars
   * available to the runtime. Use the SAAJMetaFactory instance to locate the
   * SOAPFactory implementation class.
   */
  public static SOAPFactory newInstance() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a new SOAPFactory object that is an instance of the specified
   * implementation, this method uses the SAAJMetaFactory to locate the
   * implementation class and create the SOAPFactory instance.
   */
  public static SOAPFactory newInstance(String protocol) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

}

