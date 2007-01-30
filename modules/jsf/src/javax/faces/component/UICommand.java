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

import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;

public class UICommand extends UIComponentBase
  implements ActionSource2
{
  public static final String COMPONENT_FAMILY = "javax.faces.Command";
  public static final String COMPONENT_TYPE = "javax.faces.Command";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private static final ActionListener []NULL_ACTION_LISTENERS
    = new ActionListener[0];

  private Object _value;
  private ValueExpression _valueExpr;
  
  private MethodExpression _actionExpr;

  private ActionListener []_actionListeners = NULL_ACTION_LISTENERS;

  private boolean _isImmediate;

  public UICommand()
  {
    setRendererType("javax.faces.Button");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // Properties
  //

  public Object getValue()
  {
    if (_value != null)
      return _value;
    else if (_valueExpr != null)
      return Util.eval(_valueExpr);
    else
      return null;
  }

  public void setValue(Object value)
  {
    _value = value;
  }

  //
  // Render Properties
  //

  public boolean isImmediate()
  {
    return _isImmediate;
  }

  public void setImmediate(boolean isImmediate)
  {
    _isImmediate = isImmediate;
  }

  //
  // expression map override
  //

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("value".equals(name)) {
      return _valueExpr;
    }
    else {
      return super.getValueExpression(name);
    }
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("value".equals(name)) {
      _valueExpr = expr;
    }
    else {
      super.setValueExpression(name, expr);
    }
  }

  //
  // Actions
  //

  @Override
  public void broadcast(FacesEvent event)
  {
    super.broadcast(event);

    if (event instanceof ActionEvent) {
      ActionEvent actionEvent = (ActionEvent) event;
      
      FacesContext context = FacesContext.getCurrentInstance();
      
      if (_actionExpr != null)
	_actionExpr.invoke(context.getELContext(), new Object[] { event });

      ActionListener listener = context.getApplication().getActionListener();

      if (listener != null)
	listener.processAction(actionEvent);
    }
  }

  @Override
  public void queueEvent(FacesEvent event)
  {
    if (event instanceof ActionEvent) {
      event.setPhaseId(isImmediate()
		       ? PhaseId.APPLY_REQUEST_VALUES
		       : PhaseId.INVOKE_APPLICATION);
    }

    super.queueEvent(event);
  }
  
  @Deprecated
  public MethodBinding getAction()
  {
    if (_actionExpr == null)
      return null;
    else
      return ((MethodBindingAdapter) _actionExpr).getBinding();
  }
  
  @Deprecated
  public void setAction(MethodBinding action)
  {
    if (action == null)
      throw new NullPointerException();
    
    setActionExpression(new MethodBindingAdapter(action));
  }

  @Deprecated
  public MethodBinding getActionListener()
  {
    FacesListener []listeners = getFacesListeners(FacesListener.class);

    for (int i = 0; i < listeners.length; i++) {
      if (listeners[i] instanceof ActionListenerAdapter) {
	return ((ActionListenerAdapter) listeners[i]).getBinding();
      }
    }

    return null;
  }

  @Deprecated
  public void setActionListener(MethodBinding action)
  {
    if (action == null)
      throw new NullPointerException();
    
    FacesListener []listeners = getFacesListeners(FacesListener.class);

    for (int i = 0; i < listeners.length; i++) {
      if (listeners[i] instanceof ActionListenerAdapter) {
	listeners[i] = new ActionListenerAdapter(action);
	_actionListeners = null;
	return;
      }
    }

    addActionListener(new ActionListenerAdapter(action));
  }

  public void addActionListener(ActionListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    addFacesListener(listener);
    
    _actionListeners = null;
  }

  public ActionListener []getActionListeners()
  {
    if (_actionListeners == null) {
      _actionListeners =
	(ActionListener[]) getFacesListeners(ActionListener.class);
    }

    return _actionListeners;
  }

  public void removeActionListener(ActionListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    removeFacesListener(listener);
    
    _actionListeners = null;
  }

  public MethodExpression getActionExpression()
  {
    return _actionExpr;
  }

  public void setActionExpression(MethodExpression action)
  {
    if (action == null)
      throw new NullPointerException();
    
    _actionExpr = action;
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    State state = new State();

    state._parent = super.saveState(context);
    
    state._value = _value;
    state._valueExpr = Util.save(_valueExpr, context);
    
    //state._actionExpr = Util.save(_valueExpr, context);

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    State state = (State) value;

    super.restoreState(context, state._parent);

    _value = state._value;
    _valueExpr = Util.restore(state._valueExpr, String.class, context);
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    VALUE,
  }

  private static class State implements java.io.Serializable {
    private Object _parent;
    
    private Object _value;
    private String _valueExpr;
  }

  static class MethodBindingAdapter extends MethodExpression {
    private MethodBinding _binding;

    MethodBindingAdapter(MethodBinding binding)
    {
      _binding = binding;
    }

    MethodBinding getBinding()
    {
      return _binding;
    }

    public boolean isLiteralText()
    {
      return false;
    }

    public String getExpressionString()
    {
      return _binding.getExpressionString();
    }
    
    public MethodInfo getMethodInfo(ELContext context)
      throws javax.el.PropertyNotFoundException,
	     javax.el.MethodNotFoundException,
	     ELException
    {
      throw new UnsupportedOperationException();
    }

    public Object invoke(ELContext context,
			 Object []params)
      throws javax.el.PropertyNotFoundException,
	     javax.el.MethodNotFoundException,
	     ELException
    {
      return _binding.invoke(FacesContext.getCurrentInstance(), params);
    }

    public int hashCode()
    {
      return _binding.hashCode();
    }
    
    public boolean equals(Object o)
    {
      return (this == o);
    }
  }

  static class ActionListenerAdapter implements ActionListener {
    private MethodBinding _binding;

    ActionListenerAdapter(MethodBinding binding)
    {
      _binding = binding;
    }

    MethodBinding getBinding()
    {
      return _binding;
    }

    public void processAction(ActionEvent event)
    {
      _binding.invoke(FacesContext.getCurrentInstance(),
		      new Object[] { event });
    }
  }

  static {
    _propMap.put("value", PropEnum.VALUE);
  }
}
