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

import javax.annotation.*;
import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.lifecycle.*;
import javax.faces.render.*;
import javax.faces.validator.*;

import javax.xml.bind.annotation.*;

import com.caucho.config.*;
import com.caucho.util.*;

@XmlRootElement(name="factory")
public class FactoryConfig
{
  @XmlAttribute(name="id")
  private String _id;

  private Class _applicationFactory;
  
  private Class _facesContextFactory;
  
  private Class _lifecycleFactory;
  
  private Class _renderKitFactory;

  @XmlElement(name="application-factory")
  private void setApplicationFactory(Class factory)
    throws ConfigException
  {
    Config.validate(factory, ApplicationFactory.class);

    _applicationFactory = factory;
  }
  
  private Class getApplicationFactory()
    throws ConfigException
  {
    return _applicationFactory;
  }

  @XmlElement(name="faces-context-factory")
  private void setFacesContextFactory(Class factory)
    throws ConfigException
  {
    Config.validate(factory, FacesContextFactory.class);

    _facesContextFactory = factory;
  }
  
  private Class getFacesContextFactory()
    throws ConfigException
  {
    return _facesContextFactory;
  }

  @XmlElement(name="lifecycle-factory")
  private void setLifecycleFactory(Class factory)
    throws ConfigException
  {
    Config.validate(factory, LifecycleFactory.class);

    _lifecycleFactory = factory;
  }
  
  private Class getLifecycleFactory()
    throws ConfigException
  {
    return _lifecycleFactory;
  }

  @XmlElement(name="render-kit-factory")
  private void setRenderKitFactory(Class factory)
    throws ConfigException
  {
    Config.validate(factory, RenderKitFactory.class);

    _renderKitFactory = factory;
  }

  private Class getRenderKitFactory()
    throws ConfigException
  {
    return _renderKitFactory;
  }

  @XmlElement(name="factory-extension")
  private void setFactoryExtension(BuilderProgram program)
    throws ConfigException
  {
  }

  void init()
  {
    try {
      if (_applicationFactory != null) {
	FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY,
				 _applicationFactory.getName());

      }
      
      if (_facesContextFactory != null) {
	FactoryFinder.setFactory(FactoryFinder.FACES_CONTEXT_FACTORY,
				 _facesContextFactory.getName());

      }
      
      if (_lifecycleFactory != null) {
	FactoryFinder.setFactory(FactoryFinder.LIFECYCLE_FACTORY,
				 _lifecycleFactory.getName());

      }
      
      if (_renderKitFactory != null) {
	FactoryFinder.setFactory(FactoryFinder.RENDER_KIT_FACTORY,
				 _renderKitFactory.getName());

      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }
}
