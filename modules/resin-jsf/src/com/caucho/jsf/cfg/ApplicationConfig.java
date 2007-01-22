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

package com.caucho.jsf.cfg;

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

import javax.xml.bind.annotation.*;

import com.caucho.config.*;
import com.caucho.util.*;

@XmlRootElement(name="application")
public class ApplicationConfig
{
  @XmlAttribute(name="id")
  private String _id;

  private Class _actionListener;

  @XmlElement(name="default-render-kit-it")
  private String _defaultRenderKitId;

  @XmlElement(name="message-bundle")
  private String _messageBundle;

  private Class _navigationHandler;
  
  private Class _viewHandler;
  
  private Class _stateManager;
  
  private Class _elResolver;
  
  private Class _propertyResolver;
  
  private Class _variableResolver;

  private HashMap<String,String> _resourceBundleMap
    = new HashMap<String,String>();

  @XmlElement(name="action-listener")
  private void setActionListener(Class actionListener)
    throws ConfigException
  {
    Config.validate(actionListener, ActionListener.class);

    _actionListener = actionListener;
  }

  @XmlElement(name="navigation-handler")
  private void setNavigationHandler(Class navigationHandler)
    throws ConfigException
  {
    Config.validate(navigationHandler, NavigationHandler.class);
    
    _navigationHandler = navigationHandler;
  }

  @XmlElement(name="view-handler")
  private void setViewHandler(Class viewHandler)
    throws ConfigException
  {
    Config.validate(viewHandler, ViewHandler.class);
    
    _viewHandler = viewHandler;
  }

  @XmlElement(name="state-manager")
  private void setStateManager(Class stateManager)
    throws ConfigException
  {
    Config.validate(stateManager, StateManager.class);
    
    _stateManager = stateManager;
  }

  public Class getStateManager()
  {
    return _stateManager;
  }

  @XmlElement(name="el-resolver")
  private void setElResolver(Class elResolver)
    throws ConfigException
  {
    Config.validate(elResolver, ELResolver.class);
    
    _elResolver = elResolver;
  }

  public Class getElResolver()
  {
    return _elResolver;
  }

  @XmlElement(name="property-resolver")
  private void setPropertyResolver(Class propertyResolver)
    throws ConfigException
  {
    Config.validate(propertyResolver, PropertyResolver.class);
    
    _propertyResolver = propertyResolver;
  }

  public Class getPropertyResolver()
  {
    return _propertyResolver;
  }

  @XmlElement(name="variable-resolver")
  private void setVariableResolver(Class variableResolver)
    throws ConfigException
  {
    Config.validate(variableResolver, VariableResolver.class);
    
    _variableResolver = variableResolver;
  }

  public Class getVariableResolver()
  {
    return _variableResolver;
  }

  @XmlElement(name="resource-bundle")
  private void setResourceBundle(ResourceBundleConfig bundle)
    throws ConfigException
  {
    _resourceBundleMap.put(bundle.getVar(), bundle.getBaseName());
  }

  private ResourceBundleConfig getResourceBundle()
    throws ConfigException
  {
    return null;
  }

  public HashMap<String,String> getResourceBundleMap()
  {
    return _resourceBundleMap;
  }

  @XmlElement(name="application-extension")
  private void setApplicationExtension(BuilderProgram program)
  {
  }
}
