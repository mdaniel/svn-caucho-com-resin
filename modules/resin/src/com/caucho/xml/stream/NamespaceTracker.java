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
public class NamespaceTracker {

  private Context _context = new Context();
  private int _uniquifier = 0;

  /** creates a new subcontext and enters it */
  public void push(String tagName)
  {    
    _context = _context.push(tagName);
  }

  /** deletes the current context and enters its parent */
  public void pop()
  {    
    _context = _context.pop();
  }

  public String getTagName()
  {
    return _context.getTagName();
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

  // XXX: can be vastly more efficient
  private class Context
  {
    private Context _parent;
    private String _tagName;

    private HashMap<String,String> _prefixes
      = new HashMap<String,String>();

    private HashMap<String,String> _uris
      = new HashMap<String,String>();

    public Context()
    {
      this(null, null);
    }

    public Context(Context parent, String tagName)
    {
      this._parent = parent;
      this._tagName = _tagName;
    }

    public String getTagName()
    {
      return _tagName;
    }

    public Context pop()
    {
      return _parent;
    }

    public Context push(String tagName)
    {
      return new Context(this, tagName);
    }

    /** declares a new namespace prefix in the current context */
    public void declare(String prefix, String uri)
    {
      String oldUri = getUri(prefix);
      if (uri.equals(oldUri))
        return;

      _prefixes.put(prefix, uri);
      _uris.put(uri, prefix);
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
        ws.print("\n    xmlns:");
        ws.print(Escapifier.escape(e.getKey()));
        ws.print("='");
        ws.print(Escapifier.escape(e.getValue()));
        ws.print("' ");
      }
    }
  }
}
