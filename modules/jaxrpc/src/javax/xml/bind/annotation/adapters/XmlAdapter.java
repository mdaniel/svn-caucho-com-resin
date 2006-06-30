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

package javax.xml.bind.annotation.adapters;

/**
 * Adapts a Java type for custom marshaling. Usage: Some Java types do not map
 * naturally to a XML representation, for example HashMap or other non JavaBean
 * classes. Conversely, a XML repsentation may map to a Java type but an
 * application may choose to accesss the XML representation using another Java
 * type. For example, the schema to Java binding rules bind xs:DateTime by
 * default to XmlGregorianCalendar. But an application may desire to bind
 * xs:DateTime to a custom type, MyXmlGregorianCalendar, for example. In both
 * cases, there is a mismatch between bound type , used by an application to
 * access XML content and the value type, that is mapped to an XML
 * representation. This abstract class defines methods for adapting a bound
 * type to a value type or vice versa. The methods are invoked by the JAXB
 * binding framework during marshaling and unmarshalling:
 * XmlAdapter.marshal(...): During marshalling, JAXB binding framework invokes
 * XmlAdapter.marshal(..) to adapt a bound type to value type, which is then
 * marshaled to XML representation. XmlAdapter.unmarshal(...): During
 * unmarshalling, JAXB binding framework first unmarshals XML representation to
 * a value type and then invokes XmlAdapter.unmarshal(..) to adapt the value
 * type to a bound type. Writing an adapter therefore involves the following
 * steps: Write an adapter that implements this abstract class. Install the
 * adapter using the annotation XmlJavaTypeAdapter Example: Customized mapping
 * of HashMap The following example illustrates the use of XmlAdapter and
 * XmlJavaTypeAdapter to customize the mapping of a HashMap. Step 1: Determine
 * the desired XML representation for HashMap. hashmap> entry key="id123">this
 * is a value/entry> entry key="id312">this is another value/entry> ...
 * /hashmap> Step 2: Determine the schema definition that the desired XML
 * representation shown above should follow. xs:complexType
 * name="myHashMapType"> xs:sequence> xs:element name="entry"
 * type="myHashMapEntryType" minOccurs = "0" maxOccurs="unbounded"/>
 * /xs:sequence> /xs:complexType> xs:complexType name="myHashMapEntryType">
 * xs:simpleContent> xs:extension base="xs:string"> xs:attribute name="key"
 * type="xs:int"/> /xs:extension> /xs:simpleContent> /xs:complexType> Step 3:
 * Write value types that can generate the above schema definition. public
 * class MyHashMapType { ListMyHashMapEntryType> entry; } public class
 * MyHashMapEntryType { XmlAttribute public Integer key; XmlValue public String
 * value; } Step 4: Write the adapter that adapts the value type, MyHashMapType
 * to a bound type, HashMap, used by the application. public final class
 * MyHashMapAdapter extends XmlAdapterHashMap, MyHashMapType> { ... } Step 5:
 * Use the adapter. public class Foo {
 * XmlJavaTypeAdapter(MyHashMapAdapter.class) HashMap hashmap; ... } The above
 * code fragment will map to the following schema: xs:complexType name="Foo">
 * xs:sequence> xs:element name="hashmap" type="myHashMapType" /xs:sequence>
 * /xs:complexType> Since: JAXB 2.0 Author: Sekhar Vajjhala, Sun Microsystems
 * Inc. Kohsuke Kawaguchi, Sun Microsystems Inc. See Also:XmlJavaTypeAdapter
 */
public abstract class XmlAdapter<ValueType, BoundType> {

  /**
   * Do-nothing constructor for the derived classes.
   */
  protected XmlAdapter()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convert a bound type to a value type.
   */
  public abstract ValueType marshal(BoundType v) throws Exception;


  /**
   * Convert a value type to a bound type.
   */
  public abstract BoundType unmarshal(ValueType v) throws Exception;

}

