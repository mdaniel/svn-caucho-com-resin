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
import org.w3c.dom.*;
import java.util.*;

/**
 * The JAXBContext class provides the client's entry point to the JAXB API. It
 * provides an abstraction for managing the XML/Java binding information
 * necessary to implement the JAXB binding framework operations: unmarshal,
 * marshal and validate. A client application normally obtains new instances of
 * this class using one of these two styles for newInstance methods, although
 * there are other specialized forms of the method available:
 * JAXBContext.newInstance( "com.acme.foo:com.acme.bar" ) The JAXBContext
 * instance is initialized from a list of colon separated Java package names.
 * Each java package contains JAXB mapped classes, schema-derived classes
 * and/or user annotated classes. Additionally, the java package may contain
 * JAXB package annotations that must be processed. (see JLS 3rd Edition,
 * Section 7.4.1. Package Annotations). JAXBContext.newInstance(
 * com.acme.foo.Foo.class ) The JAXBContext instance is intialized with
 * class(es) passed as parameter(s) and classes that are statically reachable
 * from these class(es). See newInstance(Class...) for details. The following
 * JAXB 1.0 requirement is only required for schema to java
 * interface/implementation binding. It does not apply to JAXB annotated
 * classes. JAXB Providers must generate a jaxb.properties file in each package
 * containing schema derived classes. The property file must contain a property
 * named javax.xml.bind.context.factory whose value is the name of the class
 * that implements the createContext APIs. The class supplied by the provider
 * does not have to be assignable to javax.xml.bind.JAXBContext, it simply has
 * to provide a class that implements the createContext APIs. In addition, the
 * provider must call the DatatypeConverter.setDatatypeConverter api prior to
 * any client invocations of the marshal and unmarshal methods. This is
 * necessary to configure the datatype converter that will be used during these
 * operations. Unmarshalling The client application may also generate Java
 * content trees explicitly rather than unmarshalling existing XML data. For
 * all JAXB-annotated value classes, an application can create content using
 * constructors. For schema-derived interface/implementation classes and for
 * the creation of elements that are not bound to a JAXB-annotated class, an
 * application needs to have access and knowledge about each of the schema
 * derived ObjectFactory classes that exist in each of java packages contained
 * in the contextPath. For each schema derived java class, there is a static
 * factory method that produces objects of that type. For example, assume that
 * after compiling a schema, you have a package com.acme.foo that contains a
 * schema derived interface named PurchaseOrder. In order to create objects of
 * that type, the client application would use the factory method like this:
 * com.acme.foo.PurchaseOrder po =
 * com.acme.foo.ObjectFactory.createPurchaseOrder(); Once the client
 * application has an instance of the the schema derived object, it can use the
 * mutator methods to set content on it. For more information on the generated
 * ObjectFactory classes, see Section 4.2 Java Package of the specification.
 * SPEC REQUIREMENT: the provider must generate a class in each package that
 * contains all of the necessary object factory methods for that package named
 * ObjectFactory as well as the static newInstance( javaContentInterface )
 * method Marshalling Here is a simple example that unmarshals an XML document
 * and then marshals it back out: JAXBContext jc = JAXBContext.newInstance(
 * "com.acme.foo" ); // unmarshal from foo.xml Unmarshaller u =
 * jc.createUnmarshaller(); FooObject fooObj = (FooObject)u.unmarshal( new
 * File( "foo.xml" ) ); // marshal to System.out Marshaller m =
 * jc.createMarshaller(); m.marshal( fooObj, System.out ); Validation JAXB
 * Runtime Binding Framework Compatibility Since: JAXB1.0 Version: $Revision:
 * 1.24 $ $Date: 2006/03/08 17:05:01 $ Author: Ryan Shoemaker, Sun
 * Microsystems, Inc.Kohsuke Kawaguchi, Sun Microsystems, Inc.Joe Fialli, Sun
 * Microsystems, Inc. See Also:Marshaller, Unmarshaller, S 7.4.1.1 "Package
 * Annotations" in Java Language Specification, 3rd Edition
 */
public abstract class JAXBContext {

  /**
   * The name of the property that contains the name of the class capable of
   * creating new JAXBContext objects. See Also:Constant Field Values
   */
  public static final String JAXB_CONTEXT_FACTORY="javax.xml.bind.context.factory";

  protected JAXBContext()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a Binder for W3C DOM.
   */
  public Binder<Node> createBinder()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a Binder object that can be used for associative/in-place
   * unmarshalling/marshalling.
   */
  public <T> Binder<T> createBinder(Class<T> domType)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a JAXBIntrospector object that can be used to introspect JAXB
   * objects.
   */
  public JAXBIntrospector createJAXBIntrospector()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a Marshaller object that can be used to convert a java content tree
   * into XML data.
   */
  public abstract Marshaller createMarshaller() throws JAXBException;


  /**
   * Create an Unmarshaller object that can be used to convert XML data into a
   * java content tree.
   */
  public abstract Unmarshaller createUnmarshaller() throws JAXBException;


  /**
   * Deprecated. has been made optional and deprecated in JAXB 2.0. Please
   * refer to the javadoc for for more detail. Create a Validator object that
   * can be used to validate a java content tree against its source schema.
   */
  public abstract Validator createValidator() throws JAXBException;


  /**
   * Generates the schema documents for this context.
   */
  public void generateSchema(SchemaOutputResolver outputResolver) throws IOException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Obtain a new instance of a JAXBContext class. The client application must
   * supply a list of classes that the new context object needs to recognize.
   * Not only the new context will recognize all the classes specified, but it
   * will also recognize any classes that are directly/indirectly referenced
   * statically from the specified classes. Subclasses of referenced classes
   * nor XmlTransient referenced classes are not registered with JAXBContext.
   * For example, in the following Java code, if you do newInstance(Foo.class),
   * the newly created JAXBContext will recognize both Foo and Bar, but not Zot
   * or FooBar: class Foo { XmlTransient FooBar c; Bar b; } class Bar { int x;
   * } class Zot extends Bar { int y; } class FooBar { } Therefore, a typical
   * client application only needs to specify the top-level classes, but it
   * needs to be careful. Note that for each java package registered with
   * JAXBContext, when the optional package annotations exist, they must be
   * processed. (see JLS 3rd Edition, Section 7.4.1. "Package Annotations").
   */
  public static JAXBContext newInstance(Class... classesToBeBound)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Obtain a new instance of a JAXBContext class. An overloading of
   * newInstance(Class...) to configure 'properties' for this instantiation of
   * JAXBContext. The interpretation of properties is implementation specific.
   */
  public static JAXBContext newInstance(Class[] classesToBeBound,
                                        Map<String,?> properties)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Obtain a new instance of a JAXBContext class. This is a convenience method
   * for the newInstance method. It uses the context class loader of the
   * current thread. To specify the use of a different class loader, either set
   * it via the Thread.setContextClassLoader() api or use the newInstance
   * method.
   */
  public static JAXBContext newInstance(String contextPath) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Obtain a new instance of a JAXBContext class. The client application must
   * supply a context path which is a list of colon (':', \u003A) separated
   * java package names that contain schema-derived classes and/or fully
   * qualified JAXB-annotated classes. Schema-derived code is registered with
   * the JAXBContext by the ObjectFactory.class generated per package.
   * Alternatively than being listed in the context path, programmer annotated
   * JAXB mapped classes can be listed in a jaxb.index resource file, format
   * described below. Note that a java package can contain both schema-derived
   * classes and user annotated JAXB classes. Additionally, the java package
   * may contain JAXB package annotations that must be processed. (see JLS 3rd
   * Edition, Section 7.4.1. "Package Annotations"). Every package listed on
   * the contextPath must meet one or both of the following conditions
   * otherwise a JAXBException will be thrown: it must contain
   * ObjectFactory.class it must contain jaxb.index Format for jaxb.index The
   * file contains a newline-separated list of class names. Space and tab
   * characters, as well as blank lines, are ignored. The comment character is
   * '#' (0x23); on each line all characters following the first comment
   * character are ignored. The file must be encoded in UTF-8. Classes that are
   * reachable, as defined in newInstance(Class...), from the listed classes
   * are also registered with JAXBContext. Constraints on class name occuring
   * in a jaxb.index file are: Must not end with ".class". Class names are
   * resolved relative to package containing jaxb.index file. Only classes
   * occuring directly in package containing jaxb.index file are allowed. Fully
   * qualified class names are not allowed. A qualified class name,relative to
   * current package, is only allowed to specify a nested or inner class. To
   * maintain compatibility with JAXB 1.0 schema to java
   * interface/implementation binding, enabled by schema customization , the
   * JAXB provider will ensure that each package on the context path has a
   * jaxb.properties file which contains a value for the
   * javax.xml.bind.context.factory property and that all values resolve to the
   * same provider. This requirement does not apply to JAXB annotated classes.
   * If there are any global XML element name collisions across the various
   * packages listed on the contextPath, a JAXBException will be thrown. Mixing
   * generated interface/impl bindings from multiple JAXB Providers in the same
   * context path may result in a JAXBException being thrown.
   */
  public static JAXBContext newInstance(String contextPath, ClassLoader classLoader) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Obtain a new instance of a JAXBContext class. This is mostly the same as
   * newInstance(String, ClassLoader), but this version allows you to pass in
   * provider-specific properties to configure the instanciation of
   * JAXBContext. The interpretation of properties is up to implementations.
   */
  public static JAXBContext newInstance(String contextPath,
                                        ClassLoader classLoader,
                                        Map<String,?> properties)
      throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

}

