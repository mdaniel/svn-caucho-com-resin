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

import com.caucho.vfs.WriteStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  Maintains a stack of namespace contexts
 */
public class NamespaceContextImpl implements NamespaceContext
{
  // The current namespace bindings
  private final HashMap<String,NamespaceBinding> _bindings
    = new HashMap<String,NamespaceBinding>();

  // The stack of element bindings
  protected final ArrayList<ElementBinding> _stack
    = new ArrayList<ElementBinding>();

  private Context _context = new Context();

  private NamespaceBinding _nullEltBinding;
  private NamespaceBinding _nullAttrBinding;

  private int _uniqueId = 0;
  private int _version = 0;

  NamespaceContextImpl()
  {
    _nullEltBinding = new NamespaceBinding(null, null, 0);
    _nullAttrBinding = new NamespaceBinding(null, null, 0);

    _stack.add(null);
  }

  /**
   * Creates a new subcontext and enters it
   */
  public void push()
  {
    _stack.add(null);
  }

  /**
   * deletes the current context and enters its parent
   */
  public void pop()
  {
    ElementBinding eltBinding = _stack.remove(_stack.size() - 1);

    if (eltBinding != null) {
      ArrayList<Decl> oldBinding = eltBinding.getOldBindingList();

      for (int i = 0; oldBinding != null && i < oldBinding.size(); i++) {
	Decl decl = oldBinding.get(i);
	NamespaceBinding binding = decl.getBinding();

	_version++;
	
	binding.setUri(decl.getOldUri());
	binding.setVersion(_version);
      }

      eltBinding.clear();
    }
  }

  public int getDepth()
  {
    return _stack.size() - 1;
  }

  /**
   * declares a new namespace prefix in the current context
   */
  public void declare(String prefix, String uri)
  {
    NamespaceBinding binding = getElementNamespace(prefix);

    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding == null) {
      eltBinding = new ElementBinding();
      
      _stack.set(_stack.size() - 1, eltBinding);
    }

    eltBinding.addOldBinding(binding, prefix, binding.getUri(), uri);

    _version++;
    binding.setUri(uri);
    binding.setVersion(_version);
  }

  /**
   *  declares a new namespace prefix in the current context; the
   *  auto-allocated prefix is returned
   */
  public String declare(String uri)
  {
    String prefix = "ns"+ _uniqueId++;
    
    declare(prefix, uri);
    
    return prefix;
  }

  /**
   * looks up the prefix, returns the uri it corresponds to
   */
  public String getUri(String prefix)
  {
    NamespaceBinding binding = _bindings.get(prefix);

    if (binding != null)
      return binding.getUri();
    else
      return null;
  }

  /**
   * looks up the uri, returns the prefix it corresponds to
   */
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
    NamespaceBinding binding = _bindings.get(prefix);

    if (binding != null)
      return binding.getUri();

    String uri = null;
    
    if (XMLConstants.XML_NS_PREFIX.equals(prefix))
      uri = XMLConstants.XML_NS_URI;
    else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
      uri = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

    return uri;
  }

  public Iterator getPrefixes(String uri)
  {
    return null;
  }

  public String getUri(int i)
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding != null) {
      return eltBinding.getOldBindingList().get(i).getNewUri();
    }
    else
      return null;
  }
  
  public String getPrefix(int i)
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding != null) {
      return eltBinding.getOldBindingList().get(i).getPrefix();
    }

    return null;
  }
  
  public int getNumDecls()
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding != null) {
      ArrayList<Decl> oldBindingList = eltBinding.getOldBindingList();

      if (oldBindingList != null)
	return oldBindingList.size();
      else
	return 0;
    }
    else
      return 0;
  }

  public void setTagName(QName tagName)
  {
    _context.setTagName(tagName);
  }

  NamespaceBinding getElementNamespace(String prefix)
  {
    NamespaceBinding binding;

    if (prefix == null)
      binding = _nullEltBinding;
    else
      binding = _bindings.get(prefix);

    if (binding != null)
      return binding;
    else {
      binding = new NamespaceBinding(prefix, null, _version);

      _bindings.put(prefix, binding);

      return binding;
    }
  }

  NamespaceBinding getAttributeNamespace(String prefix)
  {
    NamespaceBinding binding;

    if (prefix == null)
      binding = _nullAttrBinding;
    else
      binding = _bindings.get(prefix);

    if (binding != null)
      return binding;
    else {
      binding = new NamespaceBinding(prefix, null, _version);

      _bindings.put(prefix, binding);

      return binding;
    }
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

    private HashMap<String,NamespaceBinding> _bindings
      = new HashMap<String,NamespaceBinding>();

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

    /**
     * declares a new namespace prefix in the current context
     **/
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

      prefix = "ns"+(_uniqueId++);
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
        if (e.getKey() == null || "".equals(e.getKey())) {
          ws.print(" xmlns");
        }
        else {
          ws.print(" xmlns:");
          ws.print(Escapifier.escape(e.getKey()));
        }

        ws.print("='");
        ws.print(Escapifier.escape(e.getValue()));
        ws.print("'");
      }
    }
  }
  
  static class ElementBinding
  {
    private QName _name;
    private ArrayList<Decl> _declList;

    public void setName(QName name)
    {
      _name = name;
    }

    public void clear()
    {
      _declList = null;
    }

    public QName getName()
    {
      return _name;
    }
    
    public void addOldBinding(NamespaceBinding binding, String prefix,
			      String oldUri, String newUri)
    {
      if (_declList == null)
	_declList = new ArrayList<Decl>();
      
      _declList.add(new Decl(binding, prefix, oldUri, newUri));
    }

    public ArrayList<Decl> getOldBindingList()
    {
      return _declList;
    }
  }

  static class Decl {
    private final NamespaceBinding _binding;
    private final String _prefix;
    private final String _oldUri;
    private final String _newUri;

    Decl(NamespaceBinding binding, String prefix,
	 String oldUri, String newUri)
    {
      _binding = binding;
      _prefix = prefix;
      _oldUri = oldUri;
      _newUri = newUri;
    }

    NamespaceBinding getBinding()
    {
      return _binding;
    }

    String getPrefix()
    {
      return _prefix;
    }

    String getOldUri()
    {
      return _oldUri;
    }

    String getNewUri()
    {
      return _newUri;
    }
  }

  static class PrefixIterator
  {
    private NamespaceContextImpl _context;
    private ElementBinding _eltBinding;
    private int _index = 0;
      
    PrefixIterator(NamespaceContextImpl context,
		   ElementBinding eltBinding)
    {
      _context = context;
      _eltBinding = eltBinding;
    }

    public void remove()
    {
      throw new RuntimeException("not supported");
    }

    public boolean hasNext()
    {
      if (_eltBinding == null)
	return false;

      return _index < _eltBinding.getOldBindingList().size();
    }

    public Object next()
    {
      if (_eltBinding == null)
	return null;

      ArrayList<Decl> oldBindingList = _eltBinding.getOldBindingList();

      if (_index < oldBindingList.size()) {
	return oldBindingList.get(_index++).getPrefix();
      }
	
      return null;
    }
  }
}
