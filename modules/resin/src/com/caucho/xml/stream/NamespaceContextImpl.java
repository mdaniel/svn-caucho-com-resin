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
 * @author Adam Megacz
 */

package com.caucho.xml.stream;

import java.io.*;
import java.util.logging.*;
import java.util.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 *  Maintains a stack of namespace contexts
 */
public class NamespaceContextImpl implements NamespaceContext {

  private Context _context = new Context();
  private int _uniquifier = 0;

  /** creates a new subcontext and enters it */
  public void push(QName tagName)
  {    
    _context = _context.push(tagName);
  }

  /** deletes the current context and enters its parent */
  public void pop()
  {    
    _context = _context.pop();
  }

  public QName getTagName()
  {
    return _context.getTagName();
  }

  public QName resolve(String localName)
  {
    return resolve(null, localName);
  }

  public QName resolve(String prefix, String localName)
  {
    String uri = getUri(prefix);

    if (uri == null)
      return new QName(localName);

    return new QName(uri, localName, prefix);
  }

  /** declares a new namespace prefix in the current context */
  public void declare(String prefix, String uri)
  {
    _context.declare(prefix, uri);
  }

  /**
   *  declares a new namespace prefix in the current context; the
   *  auto-allocated prefix is returned
   */
  public String declare(String uri)
  {
    return _context.declare(uri);
  }

  /** looks up the prefix, returns the uri it corresponds to */
  public String getUri(String prefix)
  {
    return _context.getUri(prefix);
  }

  /** looks up the uri, returns the prefix it corresponds to */
  public String getPrefix(String uri)
  {
    return _context.getPrefix(uri);
  }

  public void emitDeclarations(WriteStream ws)
    throws IOException
  {
    _context.emitDeclarations(ws);
  }

  public String getNamespaceURI(String prefix)
  {
    return getUri(prefix);
  }

  public Iterator getPrefixes(String uri)
  {
    return _context.getPrefixes(uri);
  }

  public String getUri(int i)
  {
    return _context.getUri(i);
  }
  
  public String getPrefix(int i)
  {
    return _context.getPrefix(i);
  }
  
  public int getNumDecls()
  {
    return _context.getNumDecls();
  }

  public void setTagName(QName tagName)
  {
    _context.setTagName(tagName);
  }

  // XXX: can be vastly more efficient
  private class Context
  {
    private Context _parent;
    private QName _tagName;

    private HashMap<String,String> _prefixes
      = new HashMap<String,String>();

    private HashMap<String,String> _uris
      = new HashMap<String,String>();

    private ArrayList<String> _decls
      = new ArrayList<String>();

    public Context()
    {
      this(null, null);
    }

    public Context(Context parent, QName tagName)
    {
      this._parent = parent;
      this._tagName = tagName;
    }

    public QName getTagName()
    {
      return _tagName;
    }

    public Context pop()
    {
      return _parent;
    }

    public Iterator getPrefixes(final String uri)
    {
      return new Iterator() {

          private int i = 0;
          private Object waiting = null;

          public void remove()
          {
            throw new RuntimeException("not supported");
          }

          public boolean hasNext()
          {
            if (waiting != null) return true;
            waiting = next();
            return waiting != null;
          }

          public Object next()
          {
            if (waiting != null) {
              Object ret = waiting;
              waiting = null;
              return ret;
            }
            while (i < _decls.size())
              {
                if (uri.equals(getUri(i))) {
                  i++;
                  return getPrefix(i);
                } else {
                  i++;
                }
              }
            return null;
          }
        };
    }

    public Context push(QName tagName)
    {
      return new Context(this, tagName);
    }

    public void setTagName(QName tagName)
    {
      _tagName = tagName;
    }

    public String getUri(int i)
    {
      return getUri(_decls.get(i));
    }

    public String getPrefix(int i)
    {
      return _decls.get(i);
    }

    public int getNumDecls()
    {
      return _decls.size();
    }

    /** declares a new namespace prefix in the current context */
    public void declare(String prefix, String uri)
    {
      String oldUri = getUri(prefix);
      if (uri.equals(oldUri))
        return;

      _prefixes.put(prefix, uri);
      _uris.put(uri, prefix);
      _decls.add(prefix);
    }

    /**
     *  declares a new namespace prefix in the current context; the
     *  auto-allocated prefix is returned
     */
    public String declare(String uri)
    {
      String prefix = getPrefix(uri);
      if (prefix != null)
        return prefix;

      prefix = "ns"+(_uniquifier++);
      declare(prefix, uri);
      return prefix;
    }

    /** looks up the prefix, returns the uri it corresponds to */
    public String getUri(String prefix)
    {
      String uri = _prefixes.get(prefix);

      if (uri==null && _parent != null)
        uri = _parent.getUri(prefix);

      return uri;
    }
    
    /** looks up the uri, returns the prefix it corresponds to */
    public String getPrefix(String uri)
    {
      String prefix = _uris.get(uri);

      if (prefix==null && _parent != null)
        prefix = _parent.getPrefix(prefix);

      return prefix;
    }

    public void emitDeclarations(WriteStream ws)
      throws IOException
    {
      for(Map.Entry<String,String> e : _prefixes.entrySet()) {
        ws.print(" xmlns:");
        ws.print(Escapifier.escape(e.getKey()));
        ws.print("='");
        ws.print(Escapifier.escape(e.getValue()));
        ws.print("'");
      }
    }
  }
}
