/*
 * Copyright (c) 2001-2003 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package javax.management;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a name of an mbean.
 */
public class ObjectName implements java.io.Serializable, QueryExp {
  // The canonical name
  private String _canonicalName;
  
  // The domain of the object name
  private String _domain;

  // True if this is a pattern
  private boolean _isDomainPattern;
  
  // True if this is a property pattern
  private boolean _isPropertyPattern;

  // A hashtable of the object's properties
  private Hashtable _propertyList;

  // A string representation of the object's properties
  private String _propertyListString = "";

  /**
   * Creates an ObjectName with the given string representation.
   *
   * @param name the encoded ObjectName
   */
  public ObjectName(String name)
    throws MalformedObjectNameException
  {
    int len = name.length();
    int begin;
    int i;
    int ch = -1;

    StringBuffer sb = new StringBuffer();
    
    for (i = 0; i < len && ((ch = name.charAt(i)) != ':'); i++) {
      switch (ch) {
      case '?':
	_isDomainPattern = true;
	sb.append(name.charAt(i + 1));
	break;
	
      case '*':
	_isDomainPattern = true;
	sb.append(name.charAt(i));
	if (i != 0 || len <= i + 1 || name.charAt(i + 1) != ':')
	  throw new MalformedObjectNameException(name);
	break;
	
      default:
	sb.append((char) ch);
	break;
      }
    }

    if (ch != ':')
      throw new MalformedObjectNameException(name);
        
    _domain = sb.toString();

    if (_domain.equals(""))
      _isDomainPattern = true;
      
    _propertyList = new Hashtable();

    i++;

    if (i == len)
      throw new MalformedObjectNameException(name);

    while (i < len) {
      begin = i;

      ch = name.charAt(i);

      if (Character.isWhitespace((char) ch)) {
	i++;
	continue;
      }
      
      if (ch != '*') {
      }
      else if (i + 1 == len || name.charAt(i + 1) == ',') {
        _isPropertyPattern = true;
        i += 2;
        continue;
      }
      else
        throw new MalformedObjectNameException(name);
      
      for (; i < len && ((ch = name.charAt(i)) != '=') && ch != ' ' &&
	     ch != '\t'; i++) {
      }
      
      String key = name.substring(begin, i);

      for (; i < len && ((ch = name.charAt(i)) == ' ' || ch == '\t'); i++) {
      }

      if (ch != '=')
        throw new MalformedObjectNameException(name);
      
      for (i++; i < len && ((ch = name.charAt(i)) == ' ' || ch == '\t'); i++) {
      }

      String value;
      
      if (i < len && (ch = name.charAt(i)) == '"') {
	sb = new StringBuffer();

	for (i++; i < len && (ch = name.charAt(i)) != '"'; i++) {
	  switch (ch) {
	  case '\\':
	    sb.append((char) name.charAt(i + 1));
	    i++;
	    break;
	    
	  default:
	    sb.append((char) ch);
	    break;
	  }
	}
	i++;

	value = sb.toString();

	if (value.equals(""))
	  throw new MalformedObjectNameException(name);
      }
      else {
	begin = i;
	
	for (; i < len && ((ch = name.charAt(i)) != ','); i++) {
	  switch (ch) {
	  case ':':
	  case '"':
	  case '=':
	  case '*':
	  case '?':
	    throw new MalformedObjectNameException(name);
	  }
	}

	for (; i > begin &&
	       (name.charAt(i - 1) == ' ' || name.charAt(i - 1) == '\t');
	     i--) {
	}

	value = name.substring(begin, i);
	
	if (value.equals(""))
	  throw new MalformedObjectNameException(name);
      }

      for (; i < len && ((ch = name.charAt(i)) == ' ' || ch == '\t'); i++) {
      }

      if (ch == ',')
        i++;

      _propertyList.put(key, value);
    }

    _propertyListString = canonicalProperties(_propertyList);
    if (! _isPropertyPattern)
      _canonicalName = _domain + ':' + _propertyListString;
    else if (_propertyListString.equals(""))
      _canonicalName = _domain + ":*";
    else
      _canonicalName = _domain + ':' + _propertyListString + ",*";
  }

  /**
   * Creates an ObjectName from a domain and hashtable
   *
   * @param domain the domain
   * @param properties the table of properties
   */
  public ObjectName(String domain, Hashtable properties)
    throws MalformedObjectNameException
  {
    _domain = domain;
    _propertyList = (Hashtable) properties.clone();

    _propertyListString = canonicalProperties(_propertyList);
    _canonicalName = domain + ':' + _propertyListString;
  }

  /**
   * Creates an ObjectName from a domain and a single property
   *
   * @param domain the domain
   * @param key the propery key
   * @param value the propery value
   */
  public ObjectName(String domain, String key, String value)
    throws MalformedObjectNameException
  {
    _domain = domain;
    _propertyList = new Hashtable();
    _propertyList.put(key, value);

    _propertyListString = canonicalProperties(_propertyList);
    _canonicalName = _domain + ':' + _propertyListString;
  }

  /**
   * Creates a new ObjectName.
   *
   * @since JMX 1.2
   */
  public static ObjectName getInstance(String name)
    throws MalformedObjectNameException
  {
    return new ObjectName(name);
  }

  /**
   * Creates a new ObjectName.
   *
   * @since JMX 1.2
   */
  public static ObjectName getInstance(String domain,
				       String key,
				       String value)
    throws MalformedObjectNameException
  {
    return new ObjectName(domain, key, value);
  }

  /**
   * Creates a new ObjectName.
   *
   * @since JMX 1.2
   */
  public static ObjectName getInstance(String domain,
				       Hashtable table)
    throws MalformedObjectNameException
  {
    return new ObjectName(domain, table);
  }

  /**
   * Creates a new ObjectName.
   *
   * @since JMX 1.2
   */
  public static ObjectName getInstance(ObjectName name)
    throws MalformedObjectNameException
  {
    return name;
  }

  /**
   * Quotes the string.
   *
   * @since JMX 1.2
   */
  public static String quote(String s)
  {
    return s;
  }


  /**
   * Unquotes the string.
   *
   * @since JMX 1.2
   */
  public static String unquote(String s)
  {
    return s;
  }

  /**
   * Returns the domain of the name.
   */
  public String getDomain()
  {
    return _domain;
  }

  /**
   * Returns a property value.
   */
  public String getKeyProperty(String key)
  {
    return (String) _propertyList.get(key);
  }

  /**
   * Returns the hashtable of properties.
   */
  public Hashtable getKeyPropertyList()
  {
    return _propertyList;
  }

  /**
   * Returns a string representation of the property list.
   */
  public String getKeyPropertyListString()
  {
    return _propertyListString;
  }

  /**
   * Checks if the object name is a pattern for a query.
   */
  public boolean isPattern()
  {
    return _isDomainPattern || _isPropertyPattern;
  }

  /**
   * Checks if the object name is a pattern for a query.
   *
   * @since JMX 1.2
   */
  public boolean isDomainPattern()
  {
    return _isDomainPattern;
  }

  /**
   * Checks if the pattern is a pattern for a query.
   */
  public boolean isPropertyPattern()
  {
    return _isPropertyPattern;
  }

  /**
   * Returns the canonical name.
   */
  public String getCanonicalName()
  {
    return _canonicalName;
  }

  /**
   * Returns the canonical name.
   */
  public String getCanonicalKeyPropertyListString()
  {
    try {
      return canonicalProperties(_propertyList);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize the names.
   */
  private String canonicalProperties(Hashtable properties)
    throws MalformedObjectNameException
  {
    ArrayList list = new ArrayList();

    list.addAll(properties.keySet());

    Collections.sort(list);

    StringBuffer sb = new StringBuffer();
    
    for (int i = 0; i < list.size(); i++) {
      if (i != 0)
        sb.append(',');

      Object key = list.get(i);
      sb.append(key);
      sb.append('=');
      Object value = properties.get(key);
      if (value.equals(""))
	throw new MalformedObjectNameException("empty properties are not allowed in ObjectName");
      
      sb.append(value);
    }

    return sb.toString();
  }

  /**
   * Tests whether the ObjectName matches the name.
   */
  public boolean apply(ObjectName name)
  {
    return equals(name);
  }

  /**
   * Tests whether the ObjectName matches the name.
   */
  public void setMBeanServer(MBeanServer server)
  {
  }

  /**
   * Returns a hashCode for the objectName
   */
  public int hashCode()
  {
    return _canonicalName.hashCode();
  }

  /**
   * Returns a string representation for the name.
   */
  public String toString()
  {
    return _canonicalName;
  }

  /**
   * Returns true if the two object names are the same.
   */
  public boolean equals(Object object)
  {
    if (! (object instanceof ObjectName))
      return false;

    ObjectName name = (ObjectName) object;

    return _canonicalName.equals(name._canonicalName);
  }
}
