/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.component;

import java.beans.*;
import java.lang.reflect.*;

import java.io.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.render.*;

public abstract class UIComponentBase extends UIComponent
{
  private static final UIComponent []NULL_FACETS_AND_CHILDREN
    = new UIComponent[0];

  private static final WeakHashMap<Class,HashMap<String,Property>> _compMap
    = new WeakHashMap<Class,HashMap<String,Property>>();
  
  private String _id;
  private String _clientId;

  private UIComponent _parent;
  
  private String _rendererType;
  private boolean _isTransient;
  private boolean _isRendered = true;

  private ComponentList _children;
  private ComponentMap _facets;

  private UIComponent []_facetsAndChildren;

  private AttributeMap _attributeMap;
  private HashMap<String,ValueExpression> _exprMap;
  
  public Map<String,Object> getAttributes()
  {
    if (_attributeMap == null)
      _attributeMap = new AttributeMap(this);

    return _attributeMap;
  }
  
  @Deprecated
  public ValueBinding getValueBinding(String name)
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setValueBinding(String name, ValueBinding binding)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the value expression for an attribute
   *
   * @param name the name of the attribute to set
   * @param expr the value expression
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if (name == null)
      throw new NullPointerException();

    if (name.equals("id") || name.equals("parent"))
      throw new IllegalArgumentException();
    
    try {
      if (expr != null) {
	if (expr.isLiteralText()) {
	  getAttributes().put(name, expr.getValue(null));
	}
	else {
	  if (_exprMap == null)
	    _exprMap = new HashMap<String,ValueExpression>();
	  
	  _exprMap.put(name, expr);
	}
      }
      else if (_exprMap != null)
	_exprMap.remove(name);
    } catch (ELException e) {
      throw new FacesException(e);
    }
  }

  /**
   * Returns the value expression for an attribute
   *
   * @param name the name of the attribute to get
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if (name == null)
      throw new NullPointerException();

    
    if (_exprMap != null)
      return _exprMap.get(name);
    else
      return null;
  }

  /**
   * Returns the client-specific id for the component.
   */
  @Override
  public String getClientId(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    String parentId = null;

    for (UIComponent ptr = getParent(); ptr != null; ptr = ptr.getParent()) {
      if (ptr instanceof NamingContainer) {
	parentId = ptr.getClientId(context);
	break;
      }
    }

    String myId = getId();

    if (myId == null)
      myId = context.getViewRoot().createUniqueId();

    if (parentId != null)
      myId = parentId + NamingContainer.SEPARATOR_CHAR + myId;

    Renderer renderer = getRenderer(context);
    
    if (renderer != null)
      return renderer.convertClientId(context, myId);
    else
      return myId;
  }

  public String getFamily()
  {
    return null;
  }

  public String getId()
  {
    return _id;
  }

  public void setId(String id)
  {
    if (id == null) {
      _id = null;
      return;
    }
    
    int len = id.length();

    if (len == 0)
      throw new IllegalArgumentException();

    char ch = id.charAt(0);

    if (! ('a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z' || ch == '_'))
      throw new IllegalArgumentException();

    for (int i = 1; i < len; i++) {
      ch = id.charAt(i);
      
      if (! ('a' <= ch && ch <= 'z'
	     || 'A' <= ch && ch <= 'Z'
	     || '0' <= ch && ch <= '9'
	     || ch == '_'
	     || ch == '-'))
	throw new IllegalArgumentException();
    }

    _id = id;
  }

  public UIComponent getParent()
  {
    return _parent;
  }

  public void setParent(UIComponent parent)
  {
    _parent = parent;
  }

  public boolean isRendered()
  {
    return _isRendered;
  }

  public void setRendered(boolean isRendered)
  {
    _isRendered = isRendered;
  }

  public String getRendererType()
  {
    return _rendererType;
  }

  public void setRendererType(String rendererType)
  {
    _rendererType = rendererType;
  }

  public boolean getRendersChildren()
  {
    return false;
  }

  public List<UIComponent> getChildren()
  {
    if (_children == null)
      _children = new ComponentList(this);

    return _children;
  }

  public int getChildCount()
  {
    if (_children != null)
      return _children.size();
    else
      return 0;
  }

  public UIComponent findComponent(String expr)
  {
    return null;
  }

  public Map<String,UIComponent> getFacets()
  {
    if (_facets == null)
      _facets = new ComponentMap(this);

    return _facets;
  }

  public UIComponent getFacet(String name)
  {
    if (_facets != null)
      return _facets.get(name);
    else
      return null;
  }

  public Iterator<UIComponent> getFacetsAndChildren()
  {
    return new FacetAndChildIterator(getFacetsAndChildrenArray());
  }

  private UIComponent []getFacetsAndChildrenArray()
  {
    if (_facetsAndChildren == null) {
      if (_children == null && _facets == null)
	_facetsAndChildren = NULL_FACETS_AND_CHILDREN;
      else {
	int facetCount = getFacetCount();
	int childCount = getChildCount();
	
	_facetsAndChildren = new UIComponent[facetCount + childCount];

	int i = 0;
	if (_facets != null) {
	  for (UIComponent facet : _facets.values()) {
	    _facetsAndChildren[i++] = facet;
	  }
	}
	
	for (int j = 0; j < childCount; j++) {
	  _facetsAndChildren[i++] = _children.get(j);
	}
      }
    }

    return _facetsAndChildren;
  }

  public void broadcast(FacesEvent event)
    throws AbortProcessingException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Encodes all children
   */
  public void encodeAll(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();
    
    if (! isRendered())
      return;
    
    encodeBegin(context);

    int childCount = getChildCount();

    for (int i = 0; i < childCount; i++) {
      UIComponent child = _children.get(i);

      if (child.isRendered()) {
	child.encodeBegin(context);
	child.encodeChildren(context);
	child.encodeEnd(context);
      }
    }
    encodeEnd(context);
  }

  /**
   * Starts the output rendering for the encoding.
   */
  public void encodeBegin(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    Renderer renderer = getRenderer(context);

    if (renderer != null)
      renderer.encodeBegin(context, this);
  }

  public void encodeChildren(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    Renderer renderer = getRenderer(context);

    if (renderer != null)
      renderer.encodeChildren(context, this);
  }

  public void encodeEnd(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    Renderer renderer = getRenderer(context);

    if (renderer != null)
      renderer.encodeEnd(context, this);
  }

  protected void addFacesListener(FacesListener listener)
  {
  }

  protected FacesListener []getFacesListeners(Class cl)
  {
    throw new UnsupportedOperationException();
  }

  protected void removeFacesListener(FacesListener listener)
  {
  }

  public void queueEvent(FacesEvent event)
  {
  }

  /**
   * Recursively calls the decodes for any children, then calls
   * decode().
   */
  public void processDecodes(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    try {
      if (! isRendered())
        return;

      for (UIComponent child : getFacetsAndChildrenArray()) {
	child.processDecodes(context);
      }

      decode(context);
    } catch (RuntimeException e) {
      context.renderResponse();

      throw e;
    }
  }

  /**
   * Decodes the value of the component.
   */
  @Override
  public void decode(FacesContext context)
  {
    Renderer renderer = getRenderer(context);

    if (renderer != null)
      renderer.decode(context, this);
  }

  @Override
  public void processValidators(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered())
      return;

    for (UIComponent child : getFacetsAndChildrenArray()) {
      child.processValidators(context);
    }
  }

  public void processUpdates(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    try {
      if (! isRendered())
	return;

      for (UIComponent child : getFacetsAndChildrenArray()) {
	child.processUpdates(context);
      }
    } catch (RuntimeException e) {
      context.renderResponse();

      throw e;
    }
  }

  @Override
  protected FacesContext getFacesContext()
  {
    return FacesContext.getCurrentInstance();
  }

  @Override
  protected Renderer getRenderer(FacesContext context)
  {
    RenderKit renderKit = context.getRenderKit();

    if (renderKit != null)
      return renderKit.getRenderer(getFamily(), getRendererType());
    else
      return null;
  }

  public Object processSaveState(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    if (isTransient())
      return null;
    
    UIComponent []facetsAndChildren = getFacetsAndChildrenArray();

    Object []childSaveState = null;
      
    if (facetsAndChildren.length > 0) {
      for (int i = 0; i < facetsAndChildren.length; i++) {
	UIComponent child = facetsAndChildren[i];
	
	Object childState = child.processSaveState(context);

	if (childState != null) {
	  if (childSaveState == null)
	    childSaveState = new Object[facetsAndChildren.length];
      
	  childSaveState[i] = childState;
	}
      }
    }

    Object selfSaveState = saveState(context);

    return new UIComponentBaseState(childSaveState, selfSaveState);
  }
  
  public Object saveState(FacesContext context)
  {
    return null;
  }

  public void processRestoreState(FacesContext context,
				  Object state)
  {
  }

  public void restoreState(FacesContext context, Object state)
  {
  }

  public void setTransient(boolean isTransient)
  {
    _isTransient = isTransient;
  }

  public boolean isTransient()
  {
    return _isTransient;
  }

  private void removeChild(UIComponent child)
  {
    if (_children != null) {
      _children.remove(child);
    }
    else if (_facets != null) {
      for (Map.Entry<String,UIComponent> entry : _facets.entrySet()) {
	if (entry.getValue() == child) {
	  _facets.remove(entry.getKey());
	  break;
	}
      }
    }
  }

  private static class ComponentList extends AbstractList<UIComponent>
  {
    private ArrayList<UIComponent> _list = new ArrayList<UIComponent>();
    
    private UIComponent _parent;

    ComponentList(UIComponent parent)
    {
      _parent = parent;
    }

    @Override
    public boolean add(UIComponent o)
    {
      UIComponent child = (UIComponent) o;

      setParent(child);

      return _list.add(o);
    }

    @Override
    public void add(int i, UIComponent o)
    {
      UIComponent child = (UIComponent) o;

      _list.add(i, o);
      
      setParent(child);
    }

    @Override
    public boolean addAll(int i, Collection<? extends UIComponent> list)
    {
      boolean isChange = false;
      
      for (UIComponent child : list) {
	setParent(child);

	_list.add(i++, child);

	isChange = true;
      }

      return isChange;
    }

    @Override
    public UIComponent set(int i, UIComponent o)
    {
      UIComponent child = (UIComponent) o;

      UIComponent old = _list.set(i, o);

      if (old == o) {
      }
      else {
	setParent(child);

	if (old != null)
	  old.setParent(null);
      }

      return old;
    }

    @Override
    public UIComponent remove(int i)
    {
      UIComponent old = _list.remove(i);

      if (old != null)
	old.setParent(null);

      return old;
    }

    @Override
    public UIComponent get(int i)
    {
      return _list.get(i);
    }

    private void setParent(UIComponent child)
    {
      UIComponent parent = child.getParent();

      if (parent instanceof UIComponentBase) {
	((UIComponentBase) parent).removeChild(child);
      }

      child.setParent(_parent);
    }

    public int size()
    {
      return _list.size();
    }

    public boolean isEmpty()
    {
      return _list.isEmpty();
    }

    public Iterator<UIComponent> iterator()
    {
      return _list.iterator();
    }
  }

  private static class ComponentMap extends HashMap<String,UIComponent>
  {
    private UIComponent _parent;

    ComponentMap(UIComponent parent)
    {
      _parent = parent;
    }

    @Override
    public UIComponent put(String key, UIComponent o)
    {
      if (key == null)
	throw new NullPointerException();
      
      UIComponent child = (UIComponent) o;

      UIComponent parent = child.getParent();
      if (parent instanceof UIComponentBase) {
	((UIComponentBase) parent).removeChild(child);
      }

      child.setParent(_parent);

      UIComponent oldChild = super.put(key, o);

      if (oldChild != null && oldChild != o) {
	oldChild.setParent(null);
      }

      return oldChild;
    }

    @Override
    public UIComponent remove(Object key)
    {
      if (key == null)
	throw new NullPointerException();

      UIComponent oldChild = super.remove(key);

      if (oldChild != null) {
	oldChild.setParent(null);
      }

      return oldChild;
    }
  }

  private static class FacetAndChildIterator
    implements Iterator<UIComponent> {
    private final UIComponent []_children;
    private int _index;

    FacetAndChildIterator(UIComponent []children)
    {
      _children = children;
    }

    public boolean hasNext()
    {
      return _index < _children.length;
    }

    public UIComponent next()
    {
      if (_index < _children.length)
	return _children[_index++];
      else
	return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  private static class UIComponentBaseState implements java.io.Serializable {
    Object []_childSaveState;
    Object _selfSaveState;

    UIComponentBaseState()
    {
    }

    UIComponentBaseState(Object []childSaveState, Object selfSaveState)
    {
      _childSaveState = childSaveState;
      _selfSaveState = selfSaveState;
    }
  }

  private static class AttributeMap extends HashMap<String,Object>
  {
    private final transient HashMap<String,Property> _propertyMap;
    private Object _obj;

    AttributeMap(Object obj)
    {
      _obj = obj;
      
      Class cl = obj.getClass();
      
      synchronized (cl) {
	HashMap<String,Property> propMap = _compMap.get(cl);

	if (propMap == null) {
	  propMap = introspectComponent(cl);
	  _compMap.put(cl, propMap);
	}
      
        _propertyMap = propMap;
      }
    }

    public boolean containsKey(String name)
    {
      Property prop = _propertyMap.get(name);

      if (prop != null)
	return false;
      else
	return super.containsKey(name);
    }

    @Override
    public Object get(Object v)
    {
      String name = (String) v;
      
      Property prop = _propertyMap.get(name);

      if (prop == null)
	return super.get(name);

      Method getter = prop.getGetter();
      
      if (getter == null)
	throw new IllegalArgumentException(name + " is not readable");

      try {
	return getter.invoke(_obj);
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }

    @Override
    public Object put(String name, Object value)
    {
      if (name == null || value == null)
	throw new NullPointerException();
      
      Property prop = _propertyMap.get(name);

      if (prop == null)
	return super.put(name, value);

      if (prop.getSetter()  == null)
	throw new IllegalArgumentException(name + " is not writable");

      try {
	return prop.getSetter().invoke(_obj, value);
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }

    @Override
    public Object remove(Object name)
    {
      Property prop = _propertyMap.get(name);

      if (prop == null)
	return super.remove(name);

      throw new IllegalArgumentException(name + " cannot be removed");
    }

    private static HashMap<String,Property> introspectComponent(Class cl)
    {
      HashMap<String,Property> map = new HashMap<String,Property>();

      try {
        BeanInfo info = Introspector.getBeanInfo(cl, Object.class);

        for (PropertyDescriptor propDesc : info.getPropertyDescriptors()) {
  	  Property prop = new Property(propDesc.getReadMethod(),
				       propDesc.getWriteMethod());

	  map.put(propDesc.getName(), prop);
        }
      } catch (Exception e) {
        throw new FacesException(e);
      }

      return map;
    }
  }

  private static class Property {
    private final Method _getter;
    private final Method _setter;

    Property(Method getter, Method setter)
    {
      _getter = getter;
      _setter = setter;
    }

    public Method getGetter()
    {
      return _getter;
    }

    public Method getSetter()
    {
      return _setter;
    }
  }
}
