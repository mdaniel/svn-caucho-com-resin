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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.jaxb;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import static java.lang.Character.*;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JAXB utilities.
 */
public class JAXBUtil {
  private static final Map<Class,String> _datatypeMap
    = new HashMap<Class,String>();

  /**
   * Gets the type of a parameter.  If the type is something like Holder<T>,
   * it return T, otherwise, it returns the passed type.
   *
   **/
  public static Type getActualParameterType(Type type)
    throws JAXBException
  {
    if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) type;

      if (ptype.getRawType().equals(Holder.class)) {
        Type[] arguments = ptype.getActualTypeArguments();

        if (arguments.length != 1)
          throw new JAXBException("Holder has incorrect number of arguments");

        return arguments[0];
      }
    }

    return type;
  }

  public static void introspectClass(Class cl, 
                                     Collection<Class> jaxbClasses)
    throws JAXBException
  {
    Method[] methods = cl.getMethods();

    for (Method method : methods)
      introspectMethod(method, jaxbClasses);
  }

  /**
   * Finds all the classes mentioned in a method signature (return type and
   * parameters) and adds them to the passed in classList.  Pass in a set if
   * you expect multiple references.
   */
  public static void introspectMethod(Method method, 
                                      Collection<Class> jaxbClasses)
    throws JAXBException
  {
    introspectType(method.getReturnType(), jaxbClasses);

    Type[] params = method.getGenericParameterTypes();

    for (Type param : params) {
      if (param.equals(Holder.class))
        continue;

      introspectType(getActualParameterType(param), jaxbClasses);
    }

    /* XXX: create wrappers
    Type[] exceptions = method.getGenericExceptionTypes();

    for (Type exception : exceptions)
      introspectType(exception, jaxbClasses);*/
  }

  /**
   * Add all classes referenced by type to jaxbClasses.
   */
  private static void introspectType(Type type, Collection<Class> jaxbClasses)
  {
    if (type instanceof Class) {
      Class cl = (Class) type;

      if (! cl.isInterface())
        jaxbClasses.add((Class) type);
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      introspectType(pType.getRawType(), jaxbClasses);
      introspectType(pType.getOwnerType(), jaxbClasses);

      Type[] arguments = pType.getActualTypeArguments();

      for (Type argument : arguments)
        introspectType(argument, jaxbClasses);
    }
    else if (type != null) {
      // Type variables must be instantiated
      throw new UnsupportedOperationException("Method arguments cannot have " +
                                              "uninstantiated type variables " +
                                              "or wildcards (" + type + ")");
    }
  }

  public static String classBasename(Class cl)
  {
    int i = cl.getName().lastIndexOf('$');

    if (i < 0)
      i = cl.getName().lastIndexOf('.');

    return cl.getName().substring(i + 1);
  }

  /**
   * Tests for punctuation according to JAXB page 334.
   */
  private static boolean isPunctuation(char ch)
  {
    return "-.:\u00B7\u0387\u06DD\u06dd\u06de_".indexOf((int) ch) >= 0;
  }

  /**
   * Tests for "uncased" characters.
   */
  private static boolean isUncased(char ch)
  {
    return (! isLowerCase(ch)) && (! isUpperCase(ch));
  }

  /**
   * Splits a string into XML "words" as defined by the JAXB standard.
   * (see page 162 and appendix D)
   *
   */
  private static List<StringBuilder> splitIdentifier(String identifier)
  {
    List<StringBuilder> words = new ArrayList<StringBuilder>();
    StringBuilder word = new StringBuilder();
    char lastCh = 0;

    for (int i = 0; i < identifier.length(); i++) {
      char ch = identifier.charAt(i);

      // punctuation shouldn't be common for the java -> xml direction
      if (word.length() > 0 && isPunctuation(ch)) {
        words.add(word);
        word = new StringBuilder();
      } 
      else if (isDigit(ch)) {
        if (word.length() > 0 && ! isDigit(lastCh)) {
          words.add(word);
          word = new StringBuilder();
        }

        word.append(ch);
      }
      else if (i > 0) { // all of the following need lastCh
        if (isLowerCase(lastCh) && isUpperCase(ch)) {
          words.add(word);
          word = new StringBuilder();
          word.append(ch);
        }
        else if (isUpperCase(lastCh) && isLowerCase(ch)) {
          // need to steal the last character from the current word 
          // for the next word (e.g. FOOBar -> { "FOO", "Bar" })

          if (word.length() > 1) {
            word.deleteCharAt(word.length() - 1);
            words.add(word);
          }

          word = new StringBuilder();
          word.append(lastCh);
          word.append(ch);
        }
        else if (isLetter(lastCh) != isLetter(ch)) {
          words.add(word);
          word = new StringBuilder();
          word.append(ch);
        }
        else if (isUncased(lastCh) != isUncased(ch)) {
          words.add(word);
          word = new StringBuilder();
          word.append(ch);
        }
        else
          word.append(ch);
      }
      else
        word.append(ch);

      lastCh = ch;
    }

    if (word.length() > 0)
      words.add(word);

    return words;
  }

  public static String identifierToXmlName(Class cl)
  {
    List<StringBuilder> words = splitIdentifier(classBasename(cl));
    StringBuilder xmlName = new StringBuilder();

    xmlName.append(toLowerCase(words.get(0).charAt(0)));
    xmlName.append(words.get(0).substring(1));

    for (int i = 1; i < words.size(); i++) {
      if (words.get(i).length() > 0) {
        xmlName.append(toUpperCase(words.get(i).charAt(0)));
        xmlName.append(words.get(i).substring(1));
      }
    }

    return xmlName.toString();
  }

  public static String getXmlSchemaDatatype(Class cl)
  {
    // XXX namespaces
    
    if (_datatypeMap.containsKey(cl))
      return _datatypeMap.get(cl);

    String name = null;

    if (cl.isAnnotationPresent(XmlType.class)) {
      XmlType xmlType = (XmlType) cl.getAnnotation(XmlType.class);

      if (! "##default".equals(xmlType.name()))
        name = xmlType.name();
    }

    if (name == null)
      name = identifierToXmlName(cl);

    _datatypeMap.put(cl, name);

    return name;
  }

  public static String qNameToString(QName qName)
  {
    if (qName.getPrefix() == null || "".equals(qName.getPrefix()))
      return qName.getLocalPart();
    else
      return qName.getPrefix() + ':' + qName.getLocalPart();
  }


  static {
    _datatypeMap.put(String.class, "xsd:string");

    _datatypeMap.put(BigDecimal.class, "xsd:decimal");

    _datatypeMap.put(Boolean.class, "xsd:boolean");
    _datatypeMap.put(boolean.class, "xsd:boolean");

    _datatypeMap.put(Byte[].class, "xsd:base64Binary"); // XXX hexBinary
    _datatypeMap.put(byte[].class, "xsd:base64Binary"); // XXX hexBinary

    _datatypeMap.put(Byte.class, "xsd:byte");
    _datatypeMap.put(byte.class, "xsd:byte");

    _datatypeMap.put(Character.class, "xsd:unsignedShort");
    _datatypeMap.put(char.class, "xsd:unsignedShort");

    _datatypeMap.put(Calendar.class, "xsd:date");

    _datatypeMap.put(Double.class, "xsd:double");
    _datatypeMap.put(double.class, "xsd:double");

    _datatypeMap.put(Float.class, "xsd:float");
    _datatypeMap.put(float.class, "xsd:float");

    _datatypeMap.put(Integer.class, "xsd:int");
    _datatypeMap.put(int.class, "xsd:int");

    _datatypeMap.put(Long.class, "xsd:long");
    _datatypeMap.put(long.class, "xsd:long");

    _datatypeMap.put(Short.class, "xsd:short");
    _datatypeMap.put(short.class, "xsd:short");
  }
}
