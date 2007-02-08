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
import javax.faces.application.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.render.*;

public abstract class UIComponentBase extends UIComponent
{
  private static final UIComponent []NULL_FACETS_AND_CHILDREN
    = new UIComponent[0];
  
  private static final FacesListener []NULL_FACES_LISTENERS
    = new FacesListener[0];

  private static final HashMap<String,Integer> _rendererToCodeMap
    = new HashMap<String,Integer>();

  private static final HashMap<Integer,String> _codeToRendererMap
    = new HashMap<Integer,String>();
  
  private static final WeakHashMap<Class,HashMap<String,Property>> _compMap
    = new WeakHashMap<Class,HashMap<String,Property>>();

  private String _id;
  private String _clientId;

  private UIComponent _parent;
  
  private String _rendererType;
  private ValueExpression _rendererTypeExpr;
  
  private boolean _isTransient;
  
  private Boolean _isRendered;
  private ValueExpression _isRenderedExpr;

  private ComponentList _children;
  private ComponentMap _facets;

  private UIComponent []_facetsAndChildren;

  private AttributeMap _attributeMap;
  private HashMap<String,ValueExpression> _exprMap;
  
  private FacesListener []_facesListeners
    = NULL_FACES_LISTENERS;
  
  public Map<String,Object> getAttributes()
  {
    if (_attributeMap == null)
      _attributeMap = new AttributeMap(this);

    return _attributeMap;
  }
  
  @Deprecated
  public ValueBinding getValueBinding(String name)
  {
    ValueExpression expr = getValueExpression(name);

    if (expr == null)
      return null;
    else if (expr instanceof ValueExpressionAdapter)
      return ((ValueExpressionAdapter) expr).getBinding();
    else // XXX:
      throw new ClassCastException(ValueExpression.class.getName());
  }

  @Deprecated
  public void setValueBinding(String name, ValueBinding binding)
  {
    setValueExpression(name, new ValueExpressionAdapter(binding));
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

    if ("rendered".equals(name))
      return _isRenderedExpr;
    else if ("rendererType".equals(name))
      return _rendererTypeExpr;
    
    if (_exprMap != null)
      return _exprMap.get(name);
    else
      return null;
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
    if (name.equals("id"))
      throw new IllegalArgumentException("'id' is not a valid ValueExpression name.");
    else if (name.equals("parent"))
      throw new IllegalArgumentException("'parent' is not a valid ValueExpression name.");
    
    if ("rendered".equals(name)) {
      if (expr.isLiteralText())
	_isRendered = (Boolean) expr.getValue(null);
      else
	_isRenderedExpr = expr;
      return;
    }
    else if ("rendererType".equals(name)) {
      if (expr.isLiteralText())
	_rendererType = (String) expr.getValue(null);
      else
	_rendererTypeExpr = expr;
      return;
    }
    
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
	parentId = ptr.getContainerClientId(context);
	break;
      }
    }

    String myId = getId();

    if (myId == null) {
      myId = context.getViewRoot().createUniqueId();
      setId(myId);
    }

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
    if (_isRendered != null)
      return _isRendered;
    else if (_isRenderedExpr != null)
      return Util.evalBoolean(_isRenderedExpr);
    else
      return true;
  }

  public void setRendered(boolean isRendered)
  {
    _isRendered = isRendered;
  }

  public String getRendererType()
  {
    if (_rendererType != null)
      return _rendererType;
    else if (_rendererTypeExpr != null)
      return Util.evalString(_rendererTypeExpr);
    else
      return null;
  }

  public void setRendererType(String rendererType)
  {
    _rendererType = rendererType;
  }

  public boolean getRendersChildren()
  {
    Renderer renderer = getRenderer(FacesContext.getCurrentInstance());

    if (renderer != null)
      return renderer.getRendersChildren();
    else
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
    UIComponent base = null;

    String []values = expr.split(":");
    
    if (values[0].equals("")) {
      for (base = this; base.getParent() != null; base = base.getParent()) {
      }
    }
    else {
      for (base = this;
	   base.getParent() != null && ! (base instanceof NamingContainer);
	   base = base.getParent()) {
      }
    }

    for (int i = 0; i < values.length; i++) {
      String v = values[i];

      if ("".equals(v))
	continue;

      base = findComponent(base, v);

      if (i + 1 == values.length)
	return base;

      if (! (base instanceof NamingContainer)) {
	throw new IllegalArgumentException("'" + v + "' in expression '" + expr + "' does not match an intermediate NamingContainer.");
      }
    }
    
    return base;
  }

  private static UIComponent findComponent(UIComponent comp, String id)
  {
    if (id.equals(comp.getId()))
      return comp;
    
    /*
    UIComponent child = comp.getFacet(id);

    if (child != null)
      return child;

    int childCount = comp.getChildCount();
    if (childCount > 0) {
      List<UIComponent> children = comp.getChildren();

      for (int i = 0; i < children.size(); i++) {
	child = children.get(i);

	if (id.equals(child.getId()))
	  return child;
      }
    }
    */

    Iterator iter = comp.getFacetsAndChildren();
    while (iter.hasNext()) {
      UIComponent child = (UIComponent) iter.next();

      if (id.equals(child.getId()))
	return child;
      
      if (! (child instanceof NamingContainer)) {
	UIComponent desc = findComponent(child, id);

	if (desc != null)
	  return desc;
      }
    }

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
    for (int i = 0; i < _facesListeners.length; i++) {
      if (event.isAppropriateListener(_facesListeners[i]))
	event.processListener(_facesListeners[i]);
    }
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

    if (renderer != null && renderer.getRendersChildren())
      renderer.encodeChildren(context, this);
    else {
      int childCount = getChildCount();

      for (int i = 0; i < childCount; i++) {
	UIComponent child = _children.get(i);

	if (child.isRendered()) {
	  child.encodeBegin(context);
	  child.encodeChildren(context);
	  child.encodeEnd(context);
	}
      }
    }
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
    if (listener == null)
      throw new NullPointerException();
    
    int length = _facesListeners.length;
    
    FacesListener[] newListeners = new FacesListener[length + 1];

    System.arraycopy(_facesListeners, 0, newListeners, 0, length);

    newListeners[length] = listener;

    _facesListeners = newListeners;
  }

  protected FacesListener []getFacesListeners(Class cl)
  {
    if (FacesListener.class.equals(cl))
      return _facesListeners;

    int count = 0;
    for (int i = _facesListeners.length - 1; i >= 0; i--) {
      if (cl.isAssignableFrom(_facesListeners[i].getClass()))
	count++;
    }

    FacesListener []array = (FacesListener []) Array.newInstance(cl, count);
    count = 0;
    for (int i = _facesListeners.length - 1; i >= 0; i--) {
      if (cl.isAssignableFrom(_facesListeners[i].getClass())) {
	array[count++] = _facesListeners[i];
      }
    }

    return array;
  }

  protected void removeFacesListener(FacesListener listener)
  {
    if (listener == null)
      throw new NullPointerException();

    int length = _facesListeners.length;
    for (int i = 0; i < length; i++) {
      if (listener.equals(_facesListeners[i])) {
	FacesListener []newListeners = new FacesListener[length - 1];
	System.arraycopy(_facesListeners, 0, newListeners, 0, i);
	System.arraycopy(_facesListeners, i + 1, newListeners, i,
			 length - i - 1);

	_facesListeners = newListeners;

	return;
      }
    }
  }

  public void queueEvent(FacesEvent event)
  {
    UIComponent parent = getParent();

    if (parent != null)
      parent.queueEvent(event);
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

	if (child.isTransient())
	  continue;
	
	Object childState = child.processSaveState(context);

	if (childState != null) {
	  if (childSaveState == null)
	    childSaveState = new Object[facetsAndChildren.length];
      
	  childSaveState[i] = childState;
	}
      }
    }

    Object selfSaveState = saveState(context);

    return new Object[] { selfSaveState, childSaveState };
  }

  public void processRestoreState(FacesContext context,
				  Object state)
  {
    if (context == null)
      throw new NullPointerException();

    if (isTransient())
      return;
    
    UIComponent []facetsAndChildren = getFacetsAndChildrenArray();

    Object []baseState = (Object []) state;

    if (baseState == null)
      return;

    restoreState(context, baseState[0]);

    Object []childSaveState = (Object []) baseState[1];
      
    if (facetsAndChildren.length > 0) {
      for (int i = 0; i < facetsAndChildren.length; i++) {
	UIComponent child = facetsAndChildren[i];

	if (child.isTransient())
	  continue;

	if (childSaveState != null)
	  child.processRestoreState(context, childSaveState[i]);
	else
	  child.processRestoreState(context, null);
      }
    }
  }
  
  public Object saveState(FacesContext context)
  {
    Integer rendererCode = _rendererToCodeMap.get(_rendererType);
    String rendererString = null;

    if (rendererCode == null)
      rendererString = _rendererType;

    Object []savedListeners = null;

    if (_facesListeners.length > 0) {
      savedListeners = new Object[2 * _facesListeners.length];

      for (int i = 0; i < _facesListeners.length; i++) {
	FacesListener listener = _facesListeners[i];

	int index = 2 * i;
	
	savedListeners[index] = listener.getClass();

	if (listener instanceof StateHolder) {
	  StateHolder holder = (StateHolder) listener;
	  
	  savedListeners[index + 1] = holder.saveState(context);
	}
      }
    }
    
    return new Object[] {
      _id,
      saveExprMap(context, _exprMap),
      _isRendered,
      Util.save(_isRenderedExpr, context),
      rendererCode,
      rendererString,
      Util.save(_rendererTypeExpr, context),
      (_attributeMap != null ? _attributeMap.saveState(context) : null),
      savedListeners,
    };
  }

  public void restoreState(FacesContext context, Object stateObj)
  {
    Object []state = (Object []) stateObj;

    _id = (String) state[0];
    _exprMap = restoreExprMap(context, state[1]);

    _isRendered = (Boolean) state[2];
    _isRenderedExpr = Util.restore(state[3], Boolean.class, context);

    Integer rendererCode = (Integer) state[4];
    String rendererString = (String) state[5];
    _rendererTypeExpr = Util.restoreString(state[6], context);

    if (rendererCode != null)
      _rendererType = _codeToRendererMap.get(rendererCode);
    else
      _rendererType = rendererString;
    
    Object extMapState = state[7];

    if (extMapState != null) {
      if (_attributeMap == null)
	_attributeMap = new AttributeMap(this);

      _attributeMap.restoreState(context, extMapState);
    }

    Object []savedListeners = (Object []) state[8];

    if (savedListeners != null) {
      _facesListeners = new FacesListener[savedListeners.length / 2];

      for (int i = 0; i < _facesListeners.length; i++) {
	int index = 2 * i;
	
	Class cl = (Class) savedListeners[index];

	try {
	  FacesListener listener = (FacesListener) cl.newInstance();

	  if (listener instanceof StateHolder) {
	    StateHolder holder = (StateHolder) listener;

	    holder.restoreState(context, savedListeners[index + 1]);
	  }

	  _facesListeners[i] = listener;
	} catch (Exception e) {
	  throw new FacesException(e);
	}
      }
    }
  }

  private Object saveExprMap(FacesContext context,
			     HashMap<String,ValueExpression> exprMap)
  {
    if (exprMap == null)
      return null;

    int size = exprMap.size();
    
    Object []values = new Object[3 * size];

    int i = 0;
    for (Map.Entry<String,ValueExpression> entry : exprMap.entrySet()) {
      values[i++] = entry.getKey();

      ValueExpression expr = entry.getValue();
      values[i++] = expr.getExpressionString();
      values[i++] = expr.getExpectedType();
    }

    return values;
  }

  private HashMap<String,ValueExpression>
    restoreExprMap(FacesContext context, Object value)
  {
    if (value == null)
      return null;

    Object []state = (Object[]) value;

    HashMap<String,ValueExpression> map
      = new HashMap<String,ValueExpression>();
    
    Application app = context.getApplication();
    ExpressionFactory exprFactory = app.getExpressionFactory();

    int i = 0;
    while (i < state.length) {
      String key = (String) state[i++];
      String exprString = (String) state[i++];
      Class type = (Class) state[i++];

      ValueExpression expr
	= exprFactory.createValueExpression(context.getELContext(),
					    exprString,
					    type);

      map.put(key, expr);
    }

    return map;
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

  public static Object saveAttachedState(FacesContext context,
					 Object attachedObject)
  {
    if (attachedObject == null)
      return null;
    else if (attachedObject instanceof List) {
      List list = (List) attachedObject;

      StateList values = new StateList();
      int len = list.size();
      
      for (int i = 0; i < len; i++) {
	values.add(saveAttachedState(context, list.get(i)));
      }

      return values;
    }
    else
      return new StateHandle(context, attachedObject);
  }

  public static Object restoreAttachedState(FacesContext context,
					    Object stateObject)
  {
    if (stateObject == null)
      return null;
    else if (stateObject instanceof StateList)
      return stateObject;
    else if (stateObject instanceof StateHandle)
      return ((StateHandle) stateObject).restore(context);
    else
      throw new IllegalStateException("state object was not saved by saveAttachedState");
  }

  private static class StateList extends ArrayList {
  }
  
  private static class StateHandle implements java.io.Serializable {
    private Class _class;
    private Object _state;

    public StateHandle()
    {
    }

    public StateHandle(FacesContext context, Object value)
    {
      _class = value.getClass();

      if (value instanceof StateHolder)
	_state = ((StateHolder) value).saveState(context);
    }

    public Object restore(FacesContext context)
    {
      try {
	Object value = _class.newInstance();

	if (value instanceof StateHolder)
	  ((StateHolder) value).restoreState(context, _state);

	return value;
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  private static class ComponentList extends AbstractList<UIComponent>
    implements java.io.Serializable
  {
    private ArrayList<UIComponent> _list = new ArrayList<UIComponent>();
    
    private UIComponentBase _parent;

    ComponentList(UIComponentBase parent)
    {
      _parent = parent;
    }

    @Override
    public boolean add(UIComponent o)
    {
      UIComponent child = (UIComponent) o;

      setParent(child);

      _parent._facetsAndChildren = null;

      return _list.add(o);
    }

    @Override
    public void add(int i, UIComponent o)
    {
      UIComponent child = (UIComponent) o;

      _list.add(i, o);
      
      setParent(child);
      
      _parent._facetsAndChildren = null;
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

      _parent._facetsAndChildren = null;
      
      return isChange;
    }

    @Override
    public UIComponent set(int i, UIComponent o)
    {
      UIComponent child = (UIComponent) o;

      UIComponent old = _list.remove(i);

      if (old != null)
	old.setParent(null);
	
      setParent(child);

      _list.add(i, child);

      _parent._facetsAndChildren = null;
      
      return old;
    }

    @Override
    public UIComponent remove(int i)
    {
      UIComponent old = _list.remove(i);

      if (old != null) {
	UIComponent parent = old.getParent();
	
	old.setParent(null);
      }

      _parent._facetsAndChildren = null;
      
      return old;
    }

    @Override
    public boolean remove(Object v)
    {
      UIComponent comp = (UIComponent) v;
      
      if (_list.remove(comp)) {
	comp.setParent(null);

	_parent._facetsAndChildren = null;
      
	return true;
      }
      else
	return false;
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

  public String toString()
  {
    return getClass().getName() + "[" + getId() + "]";
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

  private static class AttributeMap extends AbstractMap<String,Object>
    implements Serializable
  {
    private final transient HashMap<String,Property> _propertyMap;
    private HashMap<String,Object> _extMap;
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

    Object saveState(FacesContext context)
    {
      return _extMap;
    }

    void restoreState(FacesContext context, Object state)
    {
      _extMap = (HashMap<String,Object>) state;
    }

    public boolean containsKey(String name)
    {
      Property prop = _propertyMap.get(name);

      if (prop != null)
	return false;
      else if (_extMap != null)
	return _extMap.containsKey(name);
      else
	return false;
    }

    @Override
    public Object get(Object v)
    {
      String name = (String) v;
      
      Property prop = _propertyMap.get(name);

      if (prop == null) {
	if (_extMap != null)
	  return _extMap.get(name);
	else
	  return null;
      }

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

      if (prop == null) {
	if (_extMap == null)
	  _extMap = new HashMap<String,Object>(8);

	return _extMap.put(name, value);
      }

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

      if (prop == null) {
	if (_extMap != null)
	  return _extMap.remove(name);
	else
	  return null;
      }

      throw new IllegalArgumentException(name + " cannot be removed");
    }

    public Set<Map.Entry<String,Object>> entrySet()
    {
      if (_extMap != null)
	return _extMap.entrySet();
      else
	return Collections.EMPTY_SET;
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

  private static class ValueExpressionAdapter extends ValueExpression
  {
    private final ValueBinding _binding;

    ValueExpressionAdapter(ValueBinding binding)
    {
      _binding = binding;
    }

    ValueBinding getBinding()
    {
      return _binding;
    }

    public Object getValue(ELContext elContext)
    {
      return _binding.getValue(FacesContext.getCurrentInstance());
    }

    public void setValue(ELContext elContext, Object value)
    {
      _binding.setValue(FacesContext.getCurrentInstance(), value);
    }

    public boolean isReadOnly(ELContext elContext)
    {
      return _binding.isReadOnly(FacesContext.getCurrentInstance());
    }

    public Class getType(ELContext elContext)
    {
      return _binding.getType(FacesContext.getCurrentInstance());
    }

    public Class getExpectedType()
    {
      return Object.class;
    }

    public boolean isLiteralText()
    {
      return false;
    }

    public int hashCode()
    {
      return _binding.getExpressionString().hashCode();
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof ValueExpression))
	return false;

      ValueExpression expr = (ValueExpression) o;
      
      return getExpressionString().equals(expr.getExpressionString());
    }

    public String getExpressionString()
    {
      return _binding.getExpressionString();
    }

    public String toString()
    {
      return "ValueExpressionAdapter[" + getExpressionString() + "]";
    }
  }

  private static final void addRendererCode(String renderer)
  {
    if (renderer == null || _rendererToCodeMap.get(renderer) != null)
      return;
    
    Integer code = _rendererToCodeMap.size() + 1;
    
    _rendererToCodeMap.put(renderer, code);
    _codeToRendererMap.put(code, renderer);
  }
  
  static {
    addRendererCode(new UIColumn().getRendererType());
    addRendererCode(new UICommand().getRendererType());
    addRendererCode(new UIData().getRendererType());
    addRendererCode(new UIForm().getRendererType());
    addRendererCode(new UIGraphic().getRendererType());
    addRendererCode(new UIInput().getRendererType());
    addRendererCode(new UIMessage().getRendererType());
    addRendererCode(new UIMessages().getRendererType());
    addRendererCode(new UINamingContainer().getRendererType());
    addRendererCode(new UIOutput().getRendererType());
    addRendererCode(new UIPanel().getRendererType());
    addRendererCode(new UIParameter().getRendererType());
    addRendererCode(new UISelectBoolean().getRendererType());
    addRendererCode(new UISelectItem().getRendererType());
    addRendererCode(new UISelectItems().getRendererType());
    addRendererCode(new UISelectMany().getRendererType());
    addRendererCode(new UISelectOne().getRendererType());
    addRendererCode(new UIViewRoot().getRendererType());
    
    addRendererCode(new HtmlColumn().getRendererType());
    addRendererCode(new HtmlCommandButton().getRendererType());
    addRendererCode(new HtmlCommandLink().getRendererType());
    addRendererCode(new HtmlDataTable().getRendererType());
    addRendererCode(new HtmlForm().getRendererType());
    addRendererCode(new HtmlGraphicImage().getRendererType());
    addRendererCode(new HtmlInputHidden().getRendererType());
    addRendererCode(new HtmlInputSecret().getRendererType());
    addRendererCode(new HtmlInputText().getRendererType());
    addRendererCode(new HtmlInputTextarea().getRendererType());
    addRendererCode(new HtmlMessage().getRendererType());
    addRendererCode(new HtmlMessages().getRendererType());
    addRendererCode(new HtmlOutputFormat().getRendererType());
    addRendererCode(new HtmlOutputLabel().getRendererType());
    addRendererCode(new HtmlOutputLink().getRendererType());
    addRendererCode(new HtmlOutputText().getRendererType());
    addRendererCode(new HtmlPanelGrid().getRendererType());
    addRendererCode(new HtmlPanelGroup().getRendererType());
    addRendererCode(new HtmlSelectBooleanCheckbox().getRendererType());
    addRendererCode(new HtmlSelectManyCheckbox().getRendererType());
    addRendererCode(new HtmlSelectManyListbox().getRendererType());
    addRendererCode(new HtmlSelectManyMenu().getRendererType());
    addRendererCode(new HtmlSelectOneListbox().getRendererType());
    addRendererCode(new HtmlSelectOneMenu().getRendererType());
    addRendererCode(new HtmlSelectOneRadio().getRendererType());
  }
}
