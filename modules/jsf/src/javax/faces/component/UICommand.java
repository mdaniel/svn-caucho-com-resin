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
import javax.faces.application.*;
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

  private Boolean _immediate;
  private ValueExpression _immediateExpr;
  
  private MethodExpression _actionExpr;
  private MethodBinding _action;

  private ActionListener []_actionListeners = NULL_ACTION_LISTENERS;

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
      return Util.eval(_valueExpr, getFacesContext());
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
    if (_immediate != null)
      return _immediate;
    else if (_immediateExpr != null)
      return Util.evalBoolean(_immediateExpr, getFacesContext());
    else
      return false;
  }

  public void setImmediate(boolean immediate)
  {
    _immediate = immediate;
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
    if ("value".equals(name))
      return _valueExpr;
    else if ("immediate".equals(name))
      return _immediateExpr;
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
    if ("value".equals(name))
      _valueExpr = expr;
    else if ("immediate".equals(name))
      _immediateExpr = expr;
    else
      super.setValueExpression(name, expr);
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

      ActionListener []listeners = getActionListeners();

      for (int i = 0; i < listeners.length; i++)
	listeners[i].processAction(actionEvent);

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
    return _action;
  }
  
  @Deprecated
  public void setAction(MethodBinding action)
  {
    if (action == null)
      throw new NullPointerException();

    _action = action;
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
    String actionExprString = null;

    if (_actionExpr != null)
      actionExprString = _actionExpr.getExpressionString();

    return new Object[] {
      super.saveState(context),
      _value,
      Util.save(_valueExpr, context),
      _immediate,
      Util.save(_immediateExpr, context),
      actionExprString,
      ((_action instanceof StateHolder)
       ? saveAttachedState(context, _action)
       : null),
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _value = state[1];
    _valueExpr = Util.restore(state[2], String.class, context);
    
    _immediate = (Boolean) state[3];
    _immediateExpr = Util.restore(state[4], Boolean.class, context);

    _actionListeners = null;

    String actionExprString = (String) state[5];

    if (actionExprString != null) {
      Application app = context.getApplication();
      ExpressionFactory factory = app.getExpressionFactory();
      
      _actionExpr = factory.createMethodExpression(context.getELContext(),
						   actionExprString,
						   Object.class,
						   new Class[0]);
    }

    if (state[6] != null)
      _action = (MethodBinding) restoreAttachedState(context, state[6]);
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    VALUE,
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

  static class ActionListenerAdapter implements ActionListener, StateHolder {
    private MethodBinding _binding;

    ActionListenerAdapter()
    {
    }

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
    
    public Object saveState(FacesContext context)
    {
      return _binding.getExpressionString();
    }

    public void restoreState(FacesContext context, Object state)
    {
      Application app = context.getApplication();
      
      String expr = (String) state;

      _binding = app.createMethodBinding(expr, new Class[] { ActionEvent.class });
    }

    public boolean isTransient()
    {
      return false;
    }

    public void setTransient(boolean isTransient)
    {
    }

    public String toString()
    {
      return "ActionListenerAdapter[" + _binding + "]";
    }
  }

  static {
    _propMap.put("value", PropEnum.VALUE);
  }
}
