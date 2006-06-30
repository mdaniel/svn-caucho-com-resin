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
import java.io.*;
import javax.xml.namespace.*;

/**
 * JAXB representation of an Xml Element. This class represents information
 * about an Xml Element from both the element declaration within a schema and
 * the element instance value within an xml document with the following
 * properties element's xml tag name value represents the element instance's
 * atttribute(s) and content model element declaration's declaredType
 * (xs:element @type attribute) scope of element declaration boolean nil
 * property. (element instance's xsi:nil attribute) The declaredType and scope
 * property are the JAXB class binding for the xml type definition. Scope is
 * either JAXBElement.GlobalScope or the Java class representing the complex
 * type definition containing the schema element declaration. There is a
 * property constraint that if value is null, then nil must be true. The
 * converse is not true to enable representing a nil element with attribute(s).
 * If nil is true, it is possible that value is non-null so it can hold the
 * value of the attributes associated with a nil element. Since: JAXB 2.0
 * Author: Kohsuke Kawaguchi, Joe Fialli See Also:Serialized Form
 */
public class JAXBElement<T> implements Serializable {

  /**
   * Java datatype binding for xml element declaration's type.
   */
  protected final Class<T> declaredType=null;


  /**
   * xml element tag name
   */
  protected final QName name=null;


  /**
   * true iff the xml element instance has xsi:nil="true".
   */
  protected boolean nil;


  /**
   * Scope of xml element declaration representing this xml element instance.
   * Can be one of the following values: - for global xml element declaration.
   * - local element declaration has a scope set to the Java class
   * representation of complex type defintion containing xml element
   * declaration.
   */
  protected final Class scope=null;


  /**
   * xml element value. Represents content model and attributes of an xml
   * element instance.
   */
  protected JAXBElement value;


  /**
   * Construct an xml element instance. Parameters:name - Java binding of xml
   * element tag namedeclaredType - Java binding of xml element declaration's
   * typescope - Java binding of scope of xml element declaration. Passing null
   * is the same as passing GlobalScope.classvalue - Java instance representing
   * xml element's value.See Also:getScope(), isTypeSubstituted()
   */
  public JAXBElement(QName name, Class<T> declaredType,
                     Class scope, JAXBElement value)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an xml element instance. This is just a convenience method for
   * new JAXBElement(name,declaredType,GlobalScope.class,value)
   */
  public JAXBElement(QName name,
                     Class<T> declaredType, JAXBElement value)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns the Java binding of the xml element declaration's type attribute.
   */
  public Class<T> getDeclaredType()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns the xml element tag name.
   */
  public QName getName()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns scope of xml element declaration.
   */
  public Class getScope()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Return the content model and attribute values for this element. See
   * isNil() for a description of a property constraint when this value is null
   */
  public JAXBElement getValue()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns true iff this xml element declaration is global.
   */
  public boolean isGlobalScope()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns true iff this element instance content model is nil. This property
   * always returns true when getValue() is null. Note that the converse is not
   * true, when this property is true, getValue() can contain a non-null value
   * for attribute(s). It is valid for a nil xml element to have attribute(s).
   */
  public boolean isNil()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns true iff this xml element instance's value has a different type
   * than xml element declaration's declared type.
   */
  public boolean isTypeSubstituted()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set whether this element has nil content.
   */
  public void setNil(boolean value)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the content model and attributes of this xml element. When this
   * property is set to null, isNil() must by true. Details of constraint are
   * described at isNil().
   */
  public void setValue(JAXBElement t)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Designates global scope for an xml element.
   */
  public static final class GlobalScope {
    public GlobalScope()
    {
      throw new UnsupportedOperationException();
    }

  }
}

