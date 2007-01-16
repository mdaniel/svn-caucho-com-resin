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
import javax.servlet.*;
import javax.servlet.jsp.*;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.jsf.cfg.*;
import com.caucho.jsf.el.*;
import com.caucho.jsf.el.JsfExpressionFactoryImpl;
import com.caucho.jsf.context.*;
import com.caucho.server.webapp.WebApp;

public class ApplicationImpl extends Application
{
  private static final L10N L = new L10N(ApplicationImpl.class);
  
  private ActionListener _actionListener;
  private StateManager _stateManager;
  private ViewHandler _viewHandler;
  private NavigationHandler _navigationHandler;
  private PropertyResolver _propertyResolver;

  private ExpressionFactory _jsfExpressionFactory;
  private FacesContextELResolver _elResolver;

  private ArrayList<Locale> _locales;
  private Locale _defaultLocale;

  private ArrayList<ELContextListener> _elContextListenerList
    = new ArrayList<ELContextListener>();

  private ELContextListener []_elContextListeners;

  private HashMap<String,String> _componentClassNameMap
    = new HashMap<String,String>();

  private HashMap<String,Class> _componentClassMap
    = new HashMap<String,Class>();

  private HashMap<String,String> _validatorClassMap
    = new HashMap<String,String>();

  private HashMap<String,String> _converterIdNameMap
    = new HashMap<String,String>();

  private HashMap<String,Class> _converterIdMap
    = new HashMap<String,Class>();

  private HashMap<Class,String> _converterClassNameMap
    = new HashMap<Class,String>();

  private HashMap<Class,Class> _converterClassMap
    = new HashMap<Class,Class>();

  private String _messageBundle;

  private boolean _isInit;

  public ApplicationImpl()
  {
    WebApp webApp = WebApp.getLocal();
    
    JspFactory jspFactory = JspFactory.getDefaultFactory();

    JspApplicationContext appContext
      = jspFactory.getJspApplicationContext(webApp);

    _jsfExpressionFactory = appContext.getExpressionFactory();
    appContext.addELResolver(FacesJspELResolver.RESOLVER);
    
    ELResolver []customResolvers = new ELResolver[0];
    _elResolver = new FacesContextELResolver(customResolvers);

    addComponent(UIInput.COMPONENT_TYPE,
		 "javax.faces.component.UIInput");

    addComponent(UIOutput.COMPONENT_TYPE,
		 "javax.faces.component.UIOutput");

    addComponent(UISelectItem.COMPONENT_TYPE,
		 "javax.faces.component.UISelectItem");

    addComponent(UIViewRoot.COMPONENT_TYPE,
		 "javax.faces.component.UIViewRoot");

    addComponent(HtmlCommandButton.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlCommandButton");

    addComponent(HtmlGraphicImage.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlGraphicImage");

    addComponent(HtmlInputHidden.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlInputHidden");

    addComponent(HtmlInputSecret.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlInputSecret");

    addComponent(HtmlInputText.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlInputText");

    addComponent(HtmlInputTextarea.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlInputTextarea");

    addComponent(HtmlOutputText.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlOutputText");

    addComponent(HtmlPanelGrid.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlPanelGrid");

    addComponent(HtmlForm.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlForm");

    addComponent(HtmlSelectBooleanCheckbox.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectBooleanCheckbox");

    addComponent(HtmlSelectManyCheckbox.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectManyCheckbox");

    addComponent(HtmlSelectOneListbox.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectOneListbox");
  }
  
  public void addManagedBean(String name, ManagedBeanConfig managedBean)
  {
    _elResolver.addManagedBean(name, managedBean);
  }

  public ActionListener getActionListener()
  {
    return _actionListener;
  }

  public void setActionListener(ActionListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
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
    return _navigationHandler;
  }

  public void setNavigationHandler(NavigationHandler handler)
  {
    if (handler == null)
      throw new NullPointerException();

    _navigationHandler = handler;
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
  public void addELResolver(ELResolver resolver)
  {
    if (_isInit)
      throw new IllegalStateException(L.l("Can't add ELResolver after Application has been initialized"));
    _elResolver.addELResolver(resolver);
  }

  /**
   * @Since 1.2
   */
  public void addELContextListener(ELContextListener listener)
  {
    _elContextListenerList.add(listener);
    _elContextListeners = null;
  }

  /**
   * @Since 1.2
   */
  public void removeELContextListener(ELContextListener listener)
  {
    _elContextListenerList.remove(listener);
    _elContextListeners = null;
  }

  /**
   * @Since 1.2
   */
  public ELContextListener []getELContextListeners()
  {
    synchronized (_elContextListenerList) {
      if (_elContextListeners == null) {
	_elContextListeners
	  = new ELContextListener[_elContextListenerList.size()];

	_elContextListenerList.toArray(_elContextListeners);
      }
    }

    return _elContextListeners;
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
    if (handler == null)
      throw new NullPointerException();
    
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
    
    synchronized (_componentClassNameMap) {
      _componentClassNameMap.put(componentType, componentClass);
    }
  }

  public UIComponent createComponent(String componentType)
    throws FacesException
  {
    if (componentType == null)
      throw new NullPointerException();
    
    Class cl = getComponentClass(componentType);

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

  private Class getComponentClass(String name)
  {
    synchronized (_componentClassMap) {
      Class cl = _componentClassMap.get(name);

      if (cl != null)
	return cl;

      String className = _componentClassNameMap.get(name);

      if (className == null)
	throw new FacesException(L.l("'{0}' is an unknown component type",
				     name));
      
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	cl = Class.forName(className, false, loader);

	Config.validate(cl, UIComponent.class);

	_componentClassMap.put(name, cl);
	
	return cl;
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }
  }

  /**
   * @Since 1.2
   */
  public UIComponent createComponent(ValueExpression componentExpr,
				     FacesContext context,
				     String componentType)
    throws FacesException
  {
    if (componentExpr == null
	|| context == null
	|| componentType == null)
      throw new NullPointerException();

    Object value = componentExpr.getValue(context.getELContext());

    if (value instanceof UIComponent)
      return (UIComponent) value;

    UIComponent component = createComponent(componentType);

    componentExpr.setValue(context.getELContext(), component);

    return component;
  }
  
  @Deprecated
  public UIComponent createComponent(ValueBinding componentBinding,
				     FacesContext context,
				     String componentType)
    throws FacesException
  {
    if (componentBinding == null
	|| context == null
	|| componentType == null)
      throw new NullPointerException();
    
    return createComponent(new ValueExpressionAdapter(componentBinding,
						      UIComponent.class),
			   context,
			   componentType);
  }

  public Iterator<String> getComponentTypes()
  {
    return _componentClassMap.keySet().iterator();
  }

  public void addConverter(String converterId,
			   String converterClass)
  {
    if (converterId == null)
      throw new NullPointerException();
    
    synchronized (_converterIdMap) {
      _converterIdNameMap.put(converterId, converterClass);
    }
  }

  public Converter createConverter(String converterId)
    throws FacesException
  {
    if (converterId == null)
      throw new NullPointerException();
    
    Class cl = getConverterIdClass(converterId);

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

  private Class getConverterIdClass(String id)
  {
    synchronized (_converterIdMap) {
      Class cl = _converterIdMap.get(id);

      if (cl != null)
	return cl;

      String className = _converterIdNameMap.get(id);

      if (className == null)
	throw new FacesException(L.l("'{0}' is an unknown converter type",
				     id));
      
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	cl = Class.forName(className, false, loader);

	Config.validate(cl, Converter.class);

	_converterIdMap.put(id, cl);
	
	return cl;
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }
  }

  public Iterator<String> getConverterIds()
  {
    return _converterIdNameMap.keySet().iterator();
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
    
    Class cl = findConverter(type);

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

  private Class findConverter(Class type)
  {
    if (type == null)
      return null;
    
    Class cl;
    
    synchronized (_converterClassMap) {
      cl = _converterClassMap.get(type);
    }

    if (cl != null)
      return cl;

    Class []interfaces = type.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      cl = findConverter(interfaces[i]);

      if (cl != null)
	return cl;
    }

    return findConverter(type.getSuperclass());
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
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = new FacesELContext(facesContext, getELResolver());

    try {
      MethodExpression expr
	= factory.createMethodExpression(elContext, ref, Object.class, param);

      return new MethodBindingAdapter(expr);
    } catch (ELException e) {
      throw new ReferenceSyntaxException(e);
    }
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
    if (validatorId == null || validatorClass == null)
      throw new NullPointerException();
    
    _validatorClassMap.put(validatorId, validatorClass);
  }

  public Validator createValidator(String validatorId)
    throws FacesException
  {
    if (validatorId == null)
      throw new NullPointerException();
    
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
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = new FacesELContext(facesContext, getELResolver());

    ValueExpression expr
      = factory.createValueExpression(elContext, ref, Object.class);

    return new ValueBindingAdapter(expr);
  }

  @Override    
  public Object evaluateExpressionGet(FacesContext context,
				      String expression,
				      Class expectedType)
  {
    ExpressionFactory factory = getExpressionFactory();

    ELContext elContext = context.getELContext();

    ValueExpression expr
      = factory.createValueExpression(elContext, expression, expectedType);

    return expr.getValue(elContext);
  }

  public void init()
  {
    _isInit = true;
  }

  public String toString()
  {
    return "ApplicationImpl[]";
  }
}
