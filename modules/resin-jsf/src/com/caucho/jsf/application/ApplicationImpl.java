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

public class ApplicationImpl extends Application
{
  private static final L10N L = new L10N(ApplicationImpl.class);
  
  private ActionListener _actionListener;
  private ViewHandler _viewHandler;
  private PropertyResolver _propertyResolver;

  private ExpressionFactory _jsfExpressionFactory;

  private HashMap<String,Class> _componentClassMap
    = new HashMap<String,Class>();

  public ApplicationImpl()
  {
    _jsfExpressionFactory = new JsfExpressionFactoryImpl();

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
    throw new UnsupportedOperationException();
  }

  public void setDefaultLocale(Locale locale)
  {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  public void setMessageBundle(String bundle)
  {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }

  public void setStateManager(StateManager manager)
  {
    throw new UnsupportedOperationException();
  }

  public void addComponent(String componentType,
			   String componentClass)
  {
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
    throw new UnsupportedOperationException();
  }

  public void addConverter(Class targetClass,
				    String converterClass)
  {
    throw new UnsupportedOperationException();
  }

  public Converter createConverter(String converterId)
    throws FacesException
  {
    throw new UnsupportedOperationException();
  }

  public Converter createConverter(Class targetClass)
    throws FacesException
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<String> getConverterIds()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<Class> getConverterTypes()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public MethodBinding createMethodBinding(String ref,
					   Class []param)
    throws ReferenceSyntaxException
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<Locale> getSupportedLocales()
  {
    throw new UnsupportedOperationException();
  }

  public void setSupportedLocales(Collection<Locale> locales)
  {
    throw new UnsupportedOperationException();
  }

  public void addValidator(String validatorId, String validatorClass)
  {
    throw new UnsupportedOperationException();
  }

  public Validator createValidator(String validatorId)
    throws FacesException
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<String> getValidatorIds()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public ValueBinding createValueBinding(String ref)
    throws ReferenceSyntaxException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "ApplicationImpl[]";
  }
}
