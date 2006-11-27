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

import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

public class UIInput extends UIOutput
  implements EditableValueHolder
{
  public static final String COMPONENT_FAMILY = "javax.faces.Input";
  public static final String COMPONENT_TYPE = "javax.faces.Input";
  public static final String CONVERSION_MESSAGE_ID
    = "javax.faces.component.UIInput.CONVERSION";
  public static final String REQUIRED_MESSAGE_ID
    = "javax.faces.component.UIInput.REQUIRED";
  public static final String UPDATE_MESSAGE_ID
    = "javax.faces.component.UIInput.UPDATE";

  private static final Validator []NULL_VALIDATORS = new Validator[0];
  
  private static final ValueChangeListener []NULL_VALUE_CHANGE_LISTENERS
    = new ValueChangeListener[0];
  
  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _requiredMessage;
  private ValueExpression _requiredMessageExpr;

  private String _converterMessage;
  private ValueExpression _converterMessageExpr;

  private String _validatorMessage;
  private ValueExpression _validatorMessageExpr;

  //

  private boolean _isValid = true;
  private boolean _isLocalValueSet;

  private boolean _isRequired;
  private boolean _isImmediate;

  private Object _submittedValue;

  private ArrayList<Validator> _validatorList;
  private Validator []_validators = NULL_VALIDATORS;

  private ArrayList<ValueChangeListener> _valueChangeListenerList;
  private ValueChangeListener []_valueChangeListeners
    = NULL_VALUE_CHANGE_LISTENERS;
  

  public UIInput()
  {
    setRendererType("javax.faces.Text");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // properties
  //

  public String getRequiredMessage()
  {
    if (_requiredMessage != null)
      return _requiredMessage;
    else if (_requiredMessageExpr != null)
      return Util.evalString(_requiredMessageExpr);
    else
      return null;
  }

  public void setRequiredMessage(String value)
  {
    _requiredMessage = value;
  }

  public String getConverterMessage()
  {
    if (_converterMessage != null)
      return _converterMessage;
    else if (_converterMessageExpr != null)
      return Util.evalString(_converterMessageExpr);
    else
      return null;
  }

  public void setConverterMessage(String value)
  {
    _converterMessage = value;
  }

  public String getValidatorMessage()
  {
    if (_validatorMessage != null)
      return _validatorMessage;
    else if (_validatorMessageExpr != null)
      return Util.evalString(_validatorMessageExpr);
    else
      return null;
  }

  public void setValidatorMessage(String value)
  {
    _validatorMessage = value;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (_propMap.get(name)) {
      case REQUIRED_MESSAGE:
	return _requiredMessageExpr;
      case CONVERTER_MESSAGE:
	return _converterMessageExpr;
      case VALIDATOR_MESSAGE:
	return _validatorMessageExpr;
      }
    }

    return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (_propMap.get(name)) {
      case REQUIRED_MESSAGE:
	if (expr != null && expr.isLiteralText())
	  _requiredMessage = (String) expr.getValue(null);
	else
	  _requiredMessageExpr = expr;
	return;
	
      case CONVERTER_MESSAGE:
	if (expr != null && expr.isLiteralText())
	  _converterMessage = (String) expr.getValue(null);
	else
	  _converterMessageExpr = expr;
	return;
	
      case VALIDATOR_MESSAGE:
	if (expr != null && expr.isLiteralText())
	  _validatorMessage = (String) expr.getValue(null);
	else
	  _validatorMessageExpr = expr;
	return;
      }
    }

    super.setValueExpression(name, expr);
  }

  //
  // EditableValueHolder properties.
  //

  public Object getSubmittedValue()
  {
    return _submittedValue;
  }

  public void setSubmittedValue(Object submittedValue)
  {
    _submittedValue = submittedValue;
  }

  public void setValue(Object value)
  {
    super.setValue(value);

    _isLocalValueSet = true;
  }

  public boolean isLocalValueSet()
  {
    return _isLocalValueSet;
  }

  public void setLocalValueSet(boolean isSet)
  {
    _isLocalValueSet = isSet;
  }

  public void resetValue()
  {
    setValue(null);
    setSubmittedValue(null);
    setLocalValueSet(false);
    setValid(true);
  }

  public boolean isValid()
  {
    return _isValid;
  }
  
  public void setValid(boolean valid)
  {
    _isValid = valid;
  }

  public boolean isRequired()
  {
    return _isRequired;
  }

  public void setRequired(boolean required)
  {
    _isRequired = required;
  }

  public boolean isImmediate()
  {
    return _isImmediate;
  }

  public void setImmediate(boolean immediate)
  {
    _isImmediate = immediate;
  }

  @Deprecated
  public MethodBinding getValidator()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setValidator(MethodBinding validator)
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public MethodBinding getValueChangeListener()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setValueChangeListener(MethodBinding binding)
  {
    throw new UnsupportedOperationException();
  }

  public void addValidator(Validator validator)
  {
    if (_validatorList == null)
      _validatorList = new ArrayList<Validator>();

    if (! _validatorList.contains(validator))
      _validatorList.add(validator);

    _validators = new Validator[_validatorList.size()];
    _validatorList.toArray(_validators);
  }

  public void removeValidator(Validator validator)
  {
    if (_validatorList == null)
      return;

    _validatorList.remove(validator);

    _validators = new Validator[_validatorList.size()];
    _validatorList.toArray(_validators);
  }

  public Validator []getValidators()
  {
    return _validators;
  }

  public void addValueChangeListener(ValueChangeListener listener)
  {
    if (_valueChangeListenerList == null)
      _valueChangeListenerList = new ArrayList<ValueChangeListener>();

    if (! _valueChangeListenerList.contains(listener))
      _valueChangeListenerList.add(listener);

    int size = _valueChangeListenerList.size();
    _valueChangeListeners = new ValueChangeListener[size];
    _valueChangeListenerList.toArray(_valueChangeListeners);
  }
  
  public void removeValueChangeListener(ValueChangeListener listener)
  {
    if (_valueChangeListenerList == null)
      return;

    _valueChangeListenerList.remove(listener);

    int size = _valueChangeListenerList.size();
    _valueChangeListeners = new ValueChangeListener[size];
    _valueChangeListenerList.toArray(_valueChangeListeners);
  }
  
  public ValueChangeListener []getValueChangeListeners()
  {
    return _valueChangeListeners;
  }

  //
  // processing
  //

  public void processUpdates(FacesContext context)
  {
    super.processUpdates(context);

    try {
      updateModel(context);
    } catch (RuntimeException e) {
      context.renderResponse();
      
      throw e;
    }
  }

  public void updateModel(FacesContext context)
  {
    if (! isValid())
      return;

    if (! isLocalValueSet())
      return;

    ValueExpression expr = getValueExpression("value");

    if (expr == null)
      return;

    try {
      expr.setValue(context.getELContext(), getLocalValue());
    } catch (RuntimeException e) {
      setValid(false);
      
      throw e;
    }
  }

  @Override
  public void processValidators(FacesContext context)
  {
    super.processValidators(context);

    try {
      if (! isImmediate())
	validate(context);
    } catch (RuntimeException e) {
      context.renderResponse();

      throw e;
    }
  }

  public void validate(FacesContext context)
  {
    Object submittedValue = getSubmittedValue();

    if (submittedValue == null)
      return;

    Object value = getConvertedValue(context, submittedValue);

    validateValue(context, value);

    if (! isValid()) {
      context.renderResponse();
      return;
    }

    Object oldValue = getValue();
    setValue(value);
    setSubmittedValue(null);

    // XXX: changes
  }

  protected Object getConvertedValue(FacesContext context,
				     Object submittedValue)
    throws ConverterException
  {
    return submittedValue;
  }

  public void validateValue(FacesContext context, Object value)
  {
    if (! isValid())
      return;

    if (isRequired() && (value == null || "".equals(value))) {
      // XXX: Message
      _isValid = false;
      return;
    }

    if (value != null && ! "".equals(value)) {
      for (Validator validator : getValidators()) {
	try {
	  validator.validate(context, this, value);
	} catch (ValidatorException e) {
	  e.printStackTrace();
	  
	  // XXX: message
	  _isValid = false;
	}
      }
    }
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    State state = new State();

    state._parent = super.saveState(context);
    
    state._requiredMessage = _requiredMessage;
    state._requiredMessageExpr = Util.save(_requiredMessageExpr, context);
    
    state._converterMessage = _converterMessage;
    state._converterMessageExpr = Util.save(_converterMessageExpr, context);
    
    state._validatorMessage = _validatorMessage;
    state._validatorMessageExpr = Util.save(_validatorMessageExpr, context);

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    State state = (State) value;

    super.restoreState(context, state._parent);

    _requiredMessage = state._requiredMessage;
    _requiredMessageExpr = Util.restore(state._requiredMessageExpr,
					String.class,
					context);

    _converterMessage = state._converterMessage;
    _converterMessageExpr = Util.restore(state._converterMessageExpr,
					String.class,
					context);

    _validatorMessage = state._validatorMessage;
    _validatorMessageExpr = Util.restore(state._validatorMessageExpr,
					String.class,
					context);
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    REQUIRED_MESSAGE,
    CONVERTER_MESSAGE,
    VALIDATOR_MESSAGE,
  }

  private static class State implements java.io.Serializable {
    private Object _parent;
    
    private String _requiredMessage;
    private String _requiredMessageExpr;
    
    private String _converterMessage;
    private String _converterMessageExpr;
    
    private String _validatorMessage;
    private String _validatorMessageExpr;
  }

  static {
    _propMap.put("requiredMessage", PropEnum.REQUIRED_MESSAGE);
    _propMap.put("converterMessage", PropEnum.CONVERTER_MESSAGE);
    _propMap.put("validatorMessage", PropEnum.VALIDATOR_MESSAGE);
  }
}
