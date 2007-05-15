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
 * @author Scott Ferguson
 */

package com.caucho.xml;

import java.io.*;
import org.w3c.dom.*;
import com.caucho.vfs.*;
import com.caucho.util.*;

public class QName implements Comparable, Serializable {
  protected static L10N L = new L10N(QName.class);
  
  private String _prefix;    // preferred prefix
  private String _localName; // the real name
  private String _namespace; // URL

  private String _fullName;  // foo:bar
  private String _canonicalName; // http://www.w3.org?bar

  public QName(String qName)
  {
    this(qName, "");
  }
  
  public QName(String qName, String namespace)
  {
    _fullName = qName.intern();

    if (namespace == null) {
      _prefix = null;
      _namespace = null;
      _localName = _fullName;
    }
    else if (namespace.equals("")) {
      _prefix = null;
      _namespace = "";
      _localName = _fullName;
    }
    else {
      _namespace = namespace.intern();
      
      int p = qName.indexOf(':');
      if (p > 0) {
	_prefix = qName.substring(0, p).intern();
	_localName = qName.substring(p + 1).intern();
      }
      else {
	_prefix = null;
	_localName = _fullName;
      }
    }
    
    if (_namespace != null)
      _canonicalName = ("{" + _namespace + "}" + _localName).intern();
    else
      _canonicalName = _fullName;
  }
    
  public QName(String prefix, String localName, String namespace)
  {
    init(prefix, localName, namespace);
  }
    
  public QName(String qName, String prefix, String localName, String namespace)
  {
    _fullName = qName.intern();
    
    if (prefix != null)
      _prefix = prefix.intern();
    
    if (localName != null)
      _localName = localName.intern();
    
    if (namespace != null)
      _namespace = namespace.intern();
    
    if (_namespace != null)
      _canonicalName = ("{" + _namespace + "}" + _localName).intern();
    else
      _canonicalName = _fullName;
  }

  private void init(String prefix, String localName, String namespace)
  {
    if (localName == null || localName.equals(""))
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR, L.l("`{0}' is an invalid XML name because the local name is empty.  XML names must be `prefix:name' or simply `name'.", prefix + ":"));

    if (prefix == null || prefix.equals(""))
      _prefix = null;
    else
      _prefix = prefix.intern();

    _localName = localName.intern();

    if (_prefix != null && _prefix != "")
      _fullName = (_prefix + ":" + localName).intern();
    else
      _fullName = _localName;

    if (namespace == null) {
      _localName = _fullName;
    }
    else if (namespace.equals("")) {
      _namespace = "";
      _localName = _fullName;
    }
    else
      _namespace = namespace.intern();

    if (_namespace != null)
      _canonicalName = ("{" + _namespace + "}" + localName).intern();
    else
      _canonicalName = _fullName;
  }

  public String getName()
  {
    return _fullName;
  }

  public String getPrefix()
  {
    return _prefix;
  }

  public String getLocalName()
  {
    return _localName;
  }

  public String getCanonicalName()
  {
    return _canonicalName;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public String getNamespaceURI()
  {
    return _namespace;
  }

  /**
   * Returns the hashcode of the qname.
   */
  public int hashCode()
  {
    return _canonicalName.hashCode();
    /*
    if (_namespace != null)
      return _localName.hashCode() * 65521 + _namespace.hashCode();
    else
      return _localName.hashCode() * 65521;
    */
  }

  /**
   * Returns true if the two qnames are equivalent.
   */
  public boolean equals(Object b)
  {
    if (this == b)
      return true;
        
    if (! (b instanceof QName))
      return false;

    QName name = (QName) b;

    return _canonicalName == name._canonicalName;
    /*
    if (_localName != name._localName)
      return false;
    
    if (_namespace != name._namespace)
      return false;

    return true;

    if (_namespace != "")
      return true;
    else
      return _prefix == bName._prefix;
    */
  }

  public int compareTo(Object b)
  {
    if (this == b)
      return 0;

    else if (! (b instanceof QName))
      return -1;

    QName name = (QName) b;

    return getCanonicalName().compareTo(name.getCanonicalName());
    /*
    int cmp = getName().compareTo(name.getName());

    if (cmp != 0)
      return cmp;
    else if (_namespace == null)
      return name._namespace == null ? 0 : -1;
    else if (name._namespace == null)
      return 1;
    else
      return _namespace.compareTo(name._namespace);
    */
  }

  public String toString()
  {
    if (_prefix != null)
      return "QName[" + _prefix + ":" + _canonicalName + "]";
    else
      return "QName[" + _canonicalName + "]";
  }
}
