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

package com.caucho.jsf.application;

import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.jsf.el.*;
import com.caucho.jsf.el.JsfExpressionFactoryImpl;
import com.caucho.jsf.context.*;

public class ApplicationImpl extends Application
{
  private static final L10N L = new L10N(ApplicationImpl.class);
  
  private ActionListener _actionListener;
  private StateManager _stateManager;
  private ViewHandler _viewHandler;
  private PropertyResolver _propertyResolver;

  private ExpressionFactory _jsfExpressionFactory;
  private ELResolver _elResolver;

  private ArrayList<Locale> _locales;
  private Locale _defaultLocale = Locale.getDefault();

  private HashMap<String,Class> _componentClassMap
    = new HashMap<String,Class>();

  private HashMap<String,String> _validatorClassMap
    = new HashMap<String,String>();

  private HashMap<String,Class> _converterIdMap
    = new HashMap<String,Class>();

  private HashMap<Class,Class> _converterClassMap
    = new HashMap<Class,Class>();

  private String _messageBundle;

  public ApplicationImpl()
  {
    _jsfExpressionFactory = new JsfExpressionFactoryImpl();

    ELResolver []customResolvers = new ELResolver[0];
    _elResolver = new FacesContextELResolver(customResolvers);

    addComponent(UIInput.COMPONENT_TYPE,
		 "javax.faces.component.UIInput");

    addComponent(UIViewRoot.COMPONENT_TYPE,
		 "javax.faces.component.UIViewRoot");

    addComponent(UIOutput.COMPONENT_TYPE,
		 "javax.faces.component.UIOutput");

    addComponent(HtmlInputSecret.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlInputSecret");

    addComponent(HtmlInputText.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlInputText");

    addComponent(HtmlOutputText.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlOutputText");

    addComponent(HtmlPanelGrid.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlPanelGrid");

    addComponent(HtmlForm.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlForm");
  }
  
  public ActionListener getActionListener()
  {
    return _actionListener;
  }

  public void setActionListener(ActionListener listener)
  {
    _actionListener = listener;
  }

  public Locale getDefaultLocale()
  {
    return _defaultLocale;
  }

  public void setDefaultLocale(Locale locale)
  {
    if (locale == null)
      throw new NullPointerException();
    
    _defaultLocale = locale;
  }

  public String getDefaultRenderKitIt()
  {
    throw new UnsupportedOperationException();
  }

  public void setDefaultRenderKitId(String renderKitId)
  {
    throw new UnsupportedOperationException();
  }

  public String getMessageBundle()
  {
    return _messageBundle;
  }

  public void setMessageBundle(String bundle)
  {
    _messageBundle = bundle;
  }

  @Override
  public ResourceBundle getResourceBundle(FacesContext context,
					  String name)
  {
    return null;
  }

  public NavigationHandler getNavigationHandler()
  {
    throw new UnsupportedOperationException();
  }

  public void setNavigationHandler(NavigationHandler handler)
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PropertyResolver getPropertyResolver()
  {
    return _propertyResolver;
  }

  @Deprecated
  public void setPropertyResolver(PropertyResolver resolver)
  {
    _propertyResolver = resolver;
  }

  @Deprecated
  public VariableResolver getVariableResolver()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setVariableResolver(VariableResolver resolver)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @Since 1.2
   */
  public ExpressionFactory getExpressionFactory()
  {
    return _jsfExpressionFactory;
  }

  @Override
  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  public ViewHandler getViewHandler()
  {
    if (_viewHandler == null)
      _viewHandler = new JspViewHandler();
    
    return _viewHandler;
  }

  public void setViewHandler(ViewHandler handler)
  {
    _viewHandler = handler;
  }

  public StateManager getStateManager()
  {
    if (_stateManager == null)
      _stateManager = new SessionStateManager();
    
    return _stateManager;
  }

  public void setStateManager(StateManager manager)
  {
    _stateManager = manager;
  }

  public void addComponent(String componentType,
			   String componentClass)
  {
    if (componentType == null)
      throw new NullPointerException();
    
    synchronized (_componentClassMap) {
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	Class cl = Class.forName(componentClass, false, loader);

	Config.validate(cl, UIComponent.class);

	_componentClassMap.put(componentType, cl);
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }
  }

  public UIComponent createComponent(String componentType)
    throws FacesException
  {
    if (componentType == null)
      throw new NullPointerException();
    
    Class cl = null;
    
    synchronized (_componentClassMap) {
      cl = _componentClassMap.get(componentType);
    }

    if (cl == null)
      throw new FacesException(L.l("'{0}' is an unknown UI componentType to create",
				   componentType));

    try {
      return (UIComponent) cl.newInstance();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new FacesException(e);
    }
  }

  
  @Deprecated
  public UIComponent createComponent(ValueBinding componentBinding,
				     FacesContext context,
				     String componentType)
    throws FacesException
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<String> getComponentTypes()
  {
    throw new UnsupportedOperationException();
  }

  public void addConverter(String converterId,
			   String converterClass)
  {
    if (converterId == null)
      throw new NullPointerException();
    
    synchronized (_converterIdMap) {
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	Class cl = Class.forName(converterClass, false, loader);

	Config.validate(cl, Converter.class);

	_converterIdMap.put(converterId, cl);
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }
  }

  public Converter createConverter(String converterId)
    throws FacesException
  {
    if (converterId == null)
      throw new NullPointerException();
    
    Class cl = null;
    
    synchronized (_converterIdMap) {
      cl = _converterIdMap.get(converterId);
    }

    if (cl == null)
      return null;

    try {
      return (Converter) cl.newInstance();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new FacesException(e);
    }
  }

  public Iterator<String> getConverterIds()
  {
    return _converterIdMap.keySet().iterator();
  }

  public void addConverter(Class type,
			   String converterClass)
  {
    if (type == null)
      throw new NullPointerException();
    
    synchronized (_converterClassMap) {
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	Class cl = Class.forName(converterClass, false, loader);

	Config.validate(cl, Converter.class);

	_converterClassMap.put(type, cl);
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }
  }

  public Converter createConverter(Class type)
    throws FacesException
  {
    if (type == null)
      throw new NullPointerException();
    
    Class cl = null;
    
    synchronized (_converterClassMap) {
      cl = _converterClassMap.get(type);
    }

    if (cl == null)
      return null;

    try {
      return (Converter) cl.newInstance();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new FacesException(e);
    }
  }

  public Iterator<Class> getConverterTypes()
  {
    return _converterClassMap.keySet().iterator();
  }

  @Deprecated
  public MethodBinding createMethodBinding(String ref,
					   Class []param)
    throws ReferenceSyntaxException
  {
    ExpressionFactory factory = getExpressionFactory();

    ELResolver elResolver = getELResolver();
    ELContext elContext = new FacesELContext(getELResolver());

    MethodExpression expr
      = factory.createMethodExpression(elContext, ref, Object.class, param);

    return new MethodBindingAdapter(expr);
  }

  public Iterator<Locale> getSupportedLocales()
  {
    if (_locales != null)
      return _locales.iterator();
    else
      return new ArrayList<Locale>().iterator();
  }

  public void setSupportedLocales(Collection<Locale> locales)
  {
    _locales = new ArrayList<Locale>(locales);
  }

  public void addValidator(String validatorId, String validatorClass)
  {
    _validatorClassMap.put(validatorId, validatorClass);
  }

  public Validator createValidator(String validatorId)
    throws FacesException
  {
    try {
      String validatorClass = _validatorClassMap.get(validatorId);
      
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      Class cl = Class.forName(validatorClass, false, loader);

      return (Validator) cl.newInstance();
    } catch (Exception e) {
      throw new FacesException(e);
    }
  }

  public Iterator<String> getValidatorIds()
  {
    return _validatorClassMap.keySet().iterator();
  }

  @Override    
  public ValueBinding createValueBinding(String ref)
    throws ReferenceSyntaxException
  {
    ExpressionFactory factory = getExpressionFactory();

    ELResolver elResolver = getELResolver();
    ELContext elContext = new FacesELContext(getELResolver());

    ValueExpression expr
      = factory.createValueExpression(elContext, ref, Object.class);

    return new ValueBindingAdapter(expr);
  }

  public String toString()
  {
    return "ApplicationImpl[]";
  }
}
