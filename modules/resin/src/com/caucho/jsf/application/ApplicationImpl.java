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
  private VariableResolver _variableResolver;

  private ExpressionFactory _jsfExpressionFactory;
  
  private FacesContextELResolver _elResolver;
  
  private JsfResourceBundleELResolver _bundleResolver
    = new JsfResourceBundleELResolver();

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

  private String _defaultRenderKitId = "HTML_BASIC";

  private String _messageBundle;

  private boolean _isInit;

  public ApplicationImpl()
  {
    WebApp webApp = WebApp.getLocal();
    
    JspFactory jspFactory = JspFactory.getDefaultFactory();

    JspApplicationContext appContext
      = jspFactory.getJspApplicationContext(webApp);

    _jsfExpressionFactory = appContext.getExpressionFactory();
    
    ELResolver []customResolvers = new ELResolver[0];
    _elResolver = new FacesContextELResolver(customResolvers,
					     _bundleResolver);
    
    appContext.addELResolver(new FacesJspELResolver(this));

    addComponent(UIColumn.COMPONENT_TYPE,
		 "javax.faces.component.UIColumn");

    addComponent(UIGraphic.COMPONENT_TYPE,
		 "javax.faces.component.UIGraphic");

    addComponent(UIInput.COMPONENT_TYPE,
		 "javax.faces.component.UIInput");

    addComponent(UINamingContainer.COMPONENT_TYPE,
		 "javax.faces.component.UINamingContainer");

    addComponent(UIOutput.COMPONENT_TYPE,
		 "javax.faces.component.UIOutput");

    addComponent(UIParameter.COMPONENT_TYPE,
		 "javax.faces.component.UIParameter");

    addComponent(UISelectBoolean.COMPONENT_TYPE,
		 "javax.faces.component.UISelectBoolean");

    addComponent(UISelectOne.COMPONENT_TYPE,
		 "javax.faces.component.UISelectOne");

    addComponent(UISelectMany.COMPONENT_TYPE,
		 "javax.faces.component.UISelectMany");

    addComponent(UISelectItem.COMPONENT_TYPE,
		 "javax.faces.component.UISelectItem");

    addComponent(UISelectItems.COMPONENT_TYPE,
		 "javax.faces.component.UISelectItems");

    addComponent(UIViewRoot.COMPONENT_TYPE,
		 "javax.faces.component.UIViewRoot");

    addComponent(HtmlCommandButton.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlCommandButton");

    addComponent(HtmlCommandLink.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlCommandLink");

    addComponent(HtmlDataTable.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlDataTable");

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

    addComponent(HtmlMessage.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlMessage");

    addComponent(HtmlMessages.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlMessages");

    addComponent(HtmlOutputLink.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlOutputLink");

    addComponent(HtmlOutputText.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlOutputText");

    addComponent(HtmlPanelGrid.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlPanelGrid");

    addComponent(HtmlPanelGroup.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlPanelGroup");

    addComponent(HtmlForm.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlForm");

    addComponent(HtmlSelectBooleanCheckbox.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectBooleanCheckbox");

    addComponent(HtmlSelectManyCheckbox.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectManyCheckbox");

    addComponent(HtmlSelectManyListbox.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectManyListbox");

    addComponent(HtmlSelectManyMenu.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectManyMenu");

    addComponent(HtmlSelectOneListbox.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectOneListbox");

    addComponent(HtmlSelectOneMenu.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectOneMenu");

    addComponent(HtmlSelectOneRadio.COMPONENT_TYPE,
		 "javax.faces.component.html.HtmlSelectOneRadio");

    addConverter(boolean.class, BooleanConverter.class.getName());
    addConverter(Boolean.class, BooleanConverter.class.getName());
    
    addConverter(char.class, CharacterConverter.class.getName());
    addConverter(Character.class, CharacterConverter.class.getName());
    
    addConverter(byte.class, ByteConverter.class.getName());
    addConverter(Byte.class, ByteConverter.class.getName());
    addConverter(short.class, ShortConverter.class.getName());
    addConverter(Short.class, ShortConverter.class.getName());
    addConverter(int.class, IntegerConverter.class.getName());
    addConverter(Integer.class, IntegerConverter.class.getName());
    addConverter(long.class, LongConverter.class.getName());
    addConverter(Long.class, LongConverter.class.getName());
    addConverter(float.class, FloatConverter.class.getName());
    addConverter(Float.class, FloatConverter.class.getName());
    addConverter(double.class, DoubleConverter.class.getName());
    addConverter(Double.class, DoubleConverter.class.getName());
    
    addConverter(DateTimeConverter.CONVERTER_ID,
		 DateTimeConverter.class.getName());
    addConverter(NumberConverter.CONVERTER_ID,
		 NumberConverter.class.getName());
    
    addValidator(DoubleRangeValidator.VALIDATOR_ID,
		 DoubleRangeValidator.class.getName());
    addValidator(LengthValidator.VALIDATOR_ID,
		 LengthValidator.class.getName());
    addValidator(LongRangeValidator.VALIDATOR_ID,
		 LongRangeValidator.class.getName());
  }
  
  public void addManagedBean(String name, ManagedBeanConfig managedBean)
  {
    _elResolver.addManagedBean(name, managedBean);
  }
  
  public void addResourceBundle(String name, ResourceBundleConfig bundle)
  {
    _bundleResolver.addBundle(name, bundle);
  }

  public ActionListener getActionListener()
  {
    if (_actionListener == null)
      _actionListener = new ActionListenerImpl();
    
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

  public String getDefaultRenderKitId()
  {
    return _defaultRenderKitId;
  }

  public void setDefaultRenderKitId(String renderKitId)
  {
    _defaultRenderKitId = renderKitId;
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
    if (_navigationHandler == null)
      _navigationHandler = new NavigationHandlerImpl();
    
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
    if (_propertyResolver == null)
      _propertyResolver = new PropertyResolverAdapter(getELResolver());
    
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
    if (_variableResolver == null)
      _variableResolver = new VariableResolverAdapter(getELResolver());

    return _variableResolver;
  }

  @Deprecated
  public void setVariableResolver(VariableResolver resolver)
  {
    _variableResolver = resolver;
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
    return _componentClassNameMap.keySet().iterator();
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

    if (param == null)
      param = new Class[0];

    if (! ref.startsWith("#{") && ! ref.endsWith("}"))
      throw new ReferenceSyntaxException(L.l("'{0}' is an illegal MethodBinding.  MethodBindings require #{...} syntax.",
					     ref));
    
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

      if (validatorClass == null)
	throw new FacesException(L.l("'{0}' is not a known validator.",
				     validatorId));
      
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      Class cl = Class.forName(validatorClass, false, loader);

      return (Validator) cl.newInstance();
    } catch (FacesException e) {
      throw e;
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

    try {
      ValueExpression expr
	= factory.createValueExpression(elContext, ref, Object.class);

      ValueBinding binding = new ValueBindingAdapter(expr);

      return binding;
    } catch (ELException e) {
      throw new ReferenceSyntaxException(e);
    }
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

  public void initRequest()
  {
    _isInit = true;
  }

  public String toString()
  {
    return "ApplicationImpl[]";
  }

  static class PropertyResolverAdapter extends PropertyResolver {
    private ELResolver _elResolver;

    PropertyResolverAdapter(ELResolver elResolver)
    {
      _elResolver = elResolver;
    }
    
    public Class getType(Object base, int index)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null)
	throw new javax.faces.el.PropertyNotFoundException("getType() has null base object");
      
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	return _elResolver.getType(context.getELContext(), base, index);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
    
    public Class getType(Object base, Object property)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null)
	throw new javax.faces.el.PropertyNotFoundException();
      
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	return _elResolver.getType(context.getELContext(), base, property);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
    
    public Object getValue(Object base, int index)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null)
	throw new javax.faces.el.PropertyNotFoundException("getValue() has null base object");
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	return _elResolver.getValue(context.getELContext(), base, index);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
    
    public Object getValue(Object base, Object property)
      throws javax.faces.el.PropertyNotFoundException
    {
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	return _elResolver.getValue(context.getELContext(), base, property);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
    
    public boolean isReadOnly(Object base, int index)
      throws javax.faces.el.PropertyNotFoundException
    {
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	return _elResolver.isReadOnly(context.getELContext(), base, index);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
    
    public boolean isReadOnly(Object base, Object property)
      throws javax.faces.el.PropertyNotFoundException
    {
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	return _elResolver.isReadOnly(context.getELContext(), base, property);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
    
    public void setValue(Object base, int index, Object value)
      throws javax.faces.el.PropertyNotFoundException
    {
      if (base == null)
	throw new javax.faces.el.PropertyNotFoundException();
      
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	_elResolver.setValue(context.getELContext(), base, index, value);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      } catch (javax.el.PropertyNotWritableException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
    
    public void setValue(Object base, Object property, Object value)
      throws javax.faces.el.PropertyNotFoundException
    {
      try {
	FacesContext context = FacesContext.getCurrentInstance();
      
	_elResolver.setValue(context.getELContext(), base, property, value);
      } catch (javax.el.PropertyNotFoundException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      } catch (javax.el.PropertyNotWritableException e) {
	throw new javax.faces.el.PropertyNotFoundException(e);
      }
    }
  }

  static class VariableResolverAdapter extends VariableResolver {
    private ELResolver _elResolver;
    
    VariableResolverAdapter(ELResolver elResolver)
    {
      _elResolver = elResolver;
    }
    
    public Object resolveVariable(FacesContext context, String value)
    {
      return _elResolver.getValue(context.getELContext(), null, value);
    }
  }
}
